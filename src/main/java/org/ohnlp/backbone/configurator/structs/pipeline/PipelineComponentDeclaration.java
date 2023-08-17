package org.ohnlp.backbone.configurator.structs.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.SimpleBooleanProperty;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.config.xlang.JavaBackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.config.xlang.PythonBackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.structs.modules.JavaModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.ModuleConfigField;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.PythonModulePipelineComponentDeclaration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class PipelineComponentDeclaration {
    private EditablePipeline parent;
    private String componentID;
    private ModulePipelineComponentDeclaration componentDef;

    private BackbonePipelineComponent instance;
    private List<ModuleConfigField> config = Collections.emptyList();

    private Map<String, Schema> stepOutput;
    private final SimpleBooleanProperty updateOutputSchemas = new SimpleBooleanProperty(false);
    private Map<String, BackbonePipelineComponentConfiguration.InputDefinition> inputs;

    public PipelineComponentDeclaration(EditablePipeline parent) {
        this.parent = parent;
        this.updateOutputSchemas.addListener((e, o, n) -> {
            if (n) {
                this.stepOutput = recalculateStepOutput();
            }
        });
    }

    public void setParent(EditablePipeline parent) {
        this.parent = parent;
    }

    public String getComponentID() {
        return componentID;
    }

    public void setComponentID(String componentID) {
        this.componentID = componentID;
    }

    public ModulePipelineComponentDeclaration getComponentDef() {
        return componentDef;
    }

    public void setComponentDef(ModulePipelineComponentDeclaration componentDef) {
        this.componentDef = componentDef;
    }

    public List<ModuleConfigField> getConfig() {
        return config;
    }

    public void setConfig(List<ModuleConfigField> config) {
        this.config = config;
    }

    public Map<String, BackbonePipelineComponentConfiguration.InputDefinition> getInputs() {
        return inputs;
    }

    public void setInputs(Map<String, BackbonePipelineComponentConfiguration.InputDefinition> inputs) {
        this.inputs = inputs;
        updateOutputSchemas.set(true);
    }

    public BackbonePipelineComponentConfiguration toBackboneConfigFormat() {
        BackbonePipelineComponentConfiguration ret = null;
        if (componentDef instanceof JavaModulePipelineComponentDeclaration) {
            ret = new JavaBackbonePipelineComponentConfiguration();
            ((JavaBackbonePipelineComponentConfiguration)ret).setClazz(((JavaModulePipelineComponentDeclaration)this.componentDef).getClazz());
        } else if (componentDef instanceof PythonModulePipelineComponentDeclaration) {
            PythonBackbonePipelineComponentConfiguration pythonComponentConf = new PythonBackbonePipelineComponentConfiguration();
            PythonModulePipelineComponentDeclaration pyCompDef = (PythonModulePipelineComponentDeclaration) componentDef;
            pythonComponentConf.setBundleName(pyCompDef.getBundle_identifier());
            pythonComponentConf.setEntryPoint(pyCompDef.getEntry_point());
            pythonComponentConf.setEntryClass(pyCompDef.getClass_name());
            ret = pythonComponentConf;
        }
        ret.setComponentID(this.componentID);
        ret.setInputs(this.inputs);
        ObjectNode configJson = JsonNodeFactory.instance.objectNode();
        this.config.forEach(f -> {
            ObjectNode curr = configJson;
            String[] path = f.getPath().split("\\.");
            JsonNode valueJSON = f.getImpl().valueToJSON();
            if (valueJSON != null && !valueJSON.isNull()) {
                for (int i = 0; i < path.length - 1; i++) {
                    if (!curr.has(path[i])) {
                        ObjectNode newNode = JsonNodeFactory.instance.objectNode();
                        curr.set(path[i], newNode);
                        curr = newNode;
                    }
                }
                curr.set(path[path.length - 1], valueJSON);
            }
        });
        ret.setConfig(configJson);
        return ret;
    }

    public Map<String, Schema> getStepOutput() {
        if (stepOutput == null) { // Might not have been initialized yet at this point, so recalculate it
            stepOutput = recalculateStepOutput();
        }
        return stepOutput;
    }

    public Map<String, Schema> recalculateStepOutput() {
        if (parent == null) { // null during initial initialization as part of config load, defer until later
            return null;
        }
        updateOutputSchemas.set(false);
        BackbonePipelineComponent<?,?> instance;
        try {
            instance = componentDef.getInstance(this, true).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (instance instanceof HasOutputs) {
            Map<String, Schema> inputSchemas = new HashMap<>();
            if (instance instanceof HasInputs) {
                this.getInputs().forEach((thisComponentTag, definition) -> {
                    String otherComponentID = definition.getComponentID();
                    String otherComponentTag = definition.getInputTag();
                    Schema sourceSchema = Schema.of(
                            Schema.Field.of(getComponentID() + "_" + thisComponentTag + "_unresolvable_fields", Schema.FieldType.STRING)
                    );
                    PipelineComponentDeclaration source = parent.getComponentByID(otherComponentID);
                    if (source != null) {
                        sourceSchema = source.getStepOutput().get(otherComponentTag);
                    }
                    inputSchemas.put(thisComponentTag, sourceSchema);
                });
            }
            try {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                Future<Map<String, Schema>> schema = executor.submit(() -> ((HasOutputs) instance).calculateOutputSchema(inputSchemas));
                Map<String, Schema> ret = schema.get(5000, TimeUnit.MILLISECONDS);
                executor.shutdownNow();
                return ret;
            } catch (Throwable t) {
                Map<String, Schema> ret = new HashMap<>();
                // Unresolvable instance due to error, create empty schema TODO
                ((HasOutputs) instance).getOutputTags().forEach(tag -> {
                    ret.put(tag, Schema.of(
                            Schema.Field.of(getComponentID() + "_" + tag + "_unresolvable_fields", Schema.FieldType.STRING)
                    ));
                });
                return ret;
            }
        } else {
            return new HashMap<>();
        }
    }

    public void setUpdateOutputSchemas(boolean updateOutputSchemas) {
        this.updateOutputSchemas.set(updateOutputSchemas);
    }

    public BackbonePipelineComponent getInstance() {
        return instance;
    }

    public void setInstance(BackbonePipelineComponent instance) {
        this.instance = instance;
    }
}


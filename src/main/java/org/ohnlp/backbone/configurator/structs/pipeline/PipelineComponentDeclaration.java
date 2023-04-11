package org.ohnlp.backbone.configurator.structs.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.structs.modules.ModuleConfigField;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;

import java.util.List;
import java.util.Map;

public class PipelineComponentDeclaration {
    private String componentID;
    private ModulePipelineComponentDeclaration componentDef;
    private List<ModuleConfigField> config;

    private Map<String, BackbonePipelineComponentConfiguration.InputDefinition> inputs;

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
    }

    public BackbonePipelineComponentConfiguration toBackboneConfigFormat() {
        BackbonePipelineComponentConfiguration ret = new BackbonePipelineComponentConfiguration();
        ret.setComponentID(this.componentID);
        ret.setInputs(this.inputs);
        ret.setClazz(this.componentDef.getClazz());
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
}

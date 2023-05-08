package org.ohnlp.backbone.configurator.structs.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.ValidationError;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.ModuleRegistry;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EditablePipeline {
    private static final Logger LOGGER = Logger.getGlobal();
    private String id;
    private String description;

    private List<PipelineComponentDeclaration> components = new ArrayList<>();
    private Map<String, PipelineComponentDeclaration> componentsByID = new HashMap<>();
    private SimpleBooleanProperty dirty = new SimpleBooleanProperty(false);
    private EditablePipeline(String id) {this.id = id;}

    public EditablePipeline withID(String id) {
        this.id = id;
        return this;
    }

    public EditablePipeline withDescription(String desc) {
        this.description = desc;
        return this;
    }

    public EditablePipeline withComponents(List<PipelineComponentDeclaration> components) {
        this.components = components;
        this.componentsByID = new HashMap<>();
        this.components.forEach(d -> {
            this.componentsByID.put(d.getComponentID(), d);
            d.setParent(this);
        });
        for (PipelineComponentDeclaration component : this.components) {
            component.setUpdateOutputSchemas(true);
        }
        return this;
    }

    public EditablePipeline addComponent(PipelineComponentDeclaration component) {
        this.components.add(component);
        this.componentsByID.put(component.getComponentID(), component);
        component.setParent(this);
        component.setUpdateOutputSchemas(true);
        // TODO  need to validate input defs for new inputs
        this.dirty.set(true);
        return this;
    }

    public EditablePipeline removeComponent(PipelineComponentDeclaration component) {
        component.setParent(null);
        this.components.remove(component);
        this.componentsByID.remove(component.getComponentID());
        // TODO impl a more efficient approach for this (e.g. by indexing input IDs beforehand on insert)
        this.components.forEach(c -> {
            if (c.getInputs() != null) {
                new HashMap<>(c.getInputs()).forEach((tag, input) -> {
                    if (input.getComponentID().equals(component.getComponentID())) {
                        c.getInputs().remove(tag);
                    }
                });
            }
        });
        this.dirty.set(true);
        return this;
    }
    private EditablePipeline setDirty(boolean dirty) {
        this.dirty.set(dirty);
        return this;
    }

    public Set<String> getAvailableInputs(PipelineComponentDeclaration componentDec) {
        Set<String> ret = new HashSet<>(componentsByID.keySet());
        ret.remove(componentDec.getComponentID());
        if (componentsByID.containsKey(componentDec.getComponentID())) {
            // TODO traverse graph down
        }
        return ret;
    }

    public PipelineComponentDeclaration getComponentByID(String inputComponentID) {
        return componentsByID.get(inputComponentID);
    }

    public List<List<PipelineComponentDeclaration>> getPipelineAsSteps() {
        // Validate inputs actually exist, if not remove
        this.components.forEach(c -> {
            if (c.getInputs() != null && c.getInputs().size() > 0) {
                HashMap<String, BackbonePipelineComponentConfiguration.InputDefinition> newInputs = new HashMap<>(c.getInputs());
                new HashMap<>(c.getInputs()).forEach((tag, def) -> {
                    if (!componentsByID.containsKey(def.getComponentID())) {
                        Logger.getGlobal().warning("An input declaration for " + tag + " is declared in " + c.getComponentID() + " from source component " + def.getComponentID() + " which does not exist. Removing");
                        newInputs.remove(tag);
                        c.setInputs(newInputs);
                        this.dirty.set(true);
                    }
                });
            }
        });
        List<List<PipelineComponentDeclaration>> ret = new ArrayList<>();
        ArrayList<PipelineComponentDeclaration> toSchedule = new ArrayList<>(this.components);
        Set<String> visited = new HashSet<>();
        int currSize = Integer.MAX_VALUE;
        while (!toSchedule.isEmpty()) {
            ArrayList<PipelineComponentDeclaration> componentsThisIteration = new ArrayList<>();
            if (toSchedule.size() == currSize) {
                throw new IllegalArgumentException("Cyclic Dependency in config, " + toSchedule.stream().map(PipelineComponentDeclaration::getComponentID).collect(Collectors.joining(",")));
            }
            currSize = toSchedule.size();
            ArrayList<PipelineComponentDeclaration> componentsNextIteration = new ArrayList<>();
            for (PipelineComponentDeclaration pc : toSchedule) {
                if (pc.getInputs() != null) {
                    List<String> requiredComponents = pc.getInputs().values().stream().map(BackbonePipelineComponentConfiguration.InputDefinition::getComponentID).collect(Collectors.toList());
                    if (!visited.containsAll(requiredComponents)) {
                        componentsNextIteration.add(pc);
                        continue;
                    }
                }
                componentsThisIteration.add(pc);
            }
            ret.add(componentsThisIteration);
            visited.addAll(componentsThisIteration.stream().map(PipelineComponentDeclaration::getComponentID).collect(Collectors.toList()));
            toSchedule = componentsNextIteration;
        }
        return ret;
    }

    public static EditablePipeline create(String id) {
        return new EditablePipeline(id);
    }

    public static EditablePipeline fromConfig(BackboneConfiguration config) {
        List<BackbonePipelineComponentConfiguration> pipelineComponents = config.getPipeline();
        // Scan for legacy (v1/v2) config and convert if necessary
        Set<String> legacyConfigs = new LinkedHashSet<>();
        if (pipelineComponents != null) {
            UUID lastUID = null;
            for (int i = 0; i < pipelineComponents.size(); i++) {
                BackbonePipelineComponentConfiguration component = pipelineComponents.get(i);
                if (i > 0 && (component.getInputs() == null || component.getInputs().isEmpty()) && HasInputs.class.isAssignableFrom(component.getClazz())) {
                    legacyConfigs.add("Pipeline Definition Index " + i + ": " + component.getClazz().getName());
                    BackbonePipelineComponentConfiguration.InputDefinition generatedDef
                            = new BackbonePipelineComponentConfiguration.InputDefinition();
                    if (lastUID != null) {
                        generatedDef.setComponentID(lastUID.toString().toLowerCase(Locale.ROOT));
                        generatedDef.setInputTag("*");
                    }
                    component.setInputs(Map.of("*", generatedDef));
                }
                if (component.getComponentID() == null || component.getComponentID().trim().length() == 0) {
                    legacyConfigs.add("Pipeline Definition Index " + i + ": " + component.getClazz().getName());
                    UUID newUID = UUID.randomUUID();
                    component.setComponentID(newUID.toString().toLowerCase(Locale.ROOT));
                    lastUID = newUID;
                }
            }
        }
        // Warn if auto-conversion occurred
        boolean dirty = false;
        if (!legacyConfigs.isEmpty()) {
            LOGGER.warning("This configuration contains legacy component declarations that were auto-converted. " +
                    "Please ensure that component input/output linkages are correct.");
            for (String c : legacyConfigs) {
                LOGGER.warning("- at " + c);
            }
            dirty = true;
        }
        List<PipelineComponentDeclaration> components = pipelineComponents.stream().map(c -> PipelineConfigUtils.fromJSONConfig(null, c)).filter(Objects::nonNull).collect(Collectors.toList());

        // And now construct pipeline with pre-constructed elements
        return EditablePipeline.create(config.getId()).withDescription(config.getDescription()).withComponents(components).setDirty(dirty);
    }


    public void renameComponent(String oldID, String newID) {
        if (newID == null) {
            newID = "Autogenerated-" + UUID.randomUUID(); // We do not allow null step IDs ever.
        }
        String finalNewID = newID;
        components.forEach(c -> {
            if (c.getComponentID() == null || c.getComponentID().equals(oldID)) {
                c.setComponentID(finalNewID);
                if (oldID != null) {
                    componentsByID.remove(oldID);
                }
                componentsByID.put(finalNewID, c);
            }
            if (c.getInputs() != null) {
                c.getInputs().forEach((tag, def) -> {
                    if (def.getComponentID().equals(oldID)) {
                        def.setComponentID(finalNewID);
                    }
                });
            }
        });
        this.dirty.set(true);
    }

    public BackboneConfiguration commit() {
        return save(null);
    }

    public BackboneConfiguration save(File out) {
        BackboneConfiguration ret = new BackboneConfiguration();
        ret.setId(this.id);
        ret.setDescription(this.description);
        ret.setPipeline(this.components.stream().map(PipelineComponentDeclaration::toBackboneConfigFormat).collect(Collectors.toList()));
        if (out != null) {
            try {
                new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(ModuleRegistry.getComponentClassLoader()))
                        .writerWithDefaultPrettyPrinter()
                        .writeValue(out, ret);
            } catch (IOException e) {
                throw new RuntimeException("Failed to Save Config", e);
            }
        }
        this.dirty.set(false);
        return ret;
    }

    public SimpleBooleanProperty dirtyProperty() {
        return this.dirty;
    }

    public List<ValidationError> validatePipeline() {
        List<ValidationError> errors = new ArrayList<>();
        List<List<PipelineComponentDeclaration>> steps = getPipelineAsSteps();
        steps.forEach(step -> {
            step.forEach(component -> {
                component.componentInstance(true);
            });
        });
        return errors;
    }
}

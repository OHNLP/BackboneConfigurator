package org.ohnlp.backbone.configurator.structs.pipeline;

import javafx.scene.control.ComboBox;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;

import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EditablePipeline {
    private static final Logger LOGGER = Logger.getGlobal();
    private String id;
    private String description;

    private List<PipelineComponentDeclaration> components = new ArrayList<>();
    private Map<String, PipelineComponentDeclaration> componentsByID = new HashMap<>();
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
        });
        return this;
    }

    public EditablePipeline addComponent(PipelineComponentDeclaration component) {
        this.components.add(component);
        this.componentsByID.put(component.getComponentID(), component);
        // TODO  need to validate input defs for new inputs
        return this;
    }

    public EditablePipeline removeComponent(PipelineComponentDeclaration component) {
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
                    generatedDef.setComponentID(lastUID.toString().toLowerCase(Locale.ROOT));
                    generatedDef.setInputTag("*");
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
        if (!legacyConfigs.isEmpty()) {
            LOGGER.warning("This configuration contains legacy component declarations that were auto-converted. " +
                    "Please ensure that component input/output linkages are correct.");
            for (String c : legacyConfigs) {
                LOGGER.warning("- at " + c);
            }
        }
        List<PipelineComponentDeclaration> components = pipelineComponents.stream().map(PipelineConfigUtils::fromJSONConfig).collect(Collectors.toList());

        // And now construct pipeline with pre-constructed elements
        return EditablePipeline.create(config.getId()).withDescription(config.getDescription()).withComponents(components);
    }
}

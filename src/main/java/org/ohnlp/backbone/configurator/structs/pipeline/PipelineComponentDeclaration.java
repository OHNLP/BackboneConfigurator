package org.ohnlp.backbone.configurator.structs.pipeline;

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
}

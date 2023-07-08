package org.ohnlp.backbone.configurator.structs.modules;

import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.util.ArrayList;
import java.util.List;

public abstract class ModulePipelineComponentDeclaration {
    private String name = "";
    private String desc = "";
    private String[] requires = {};
    private final List<ModuleConfigField> configFields= new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String[] getRequires() {
        return requires;
    }

    public void setRequires(String[] requires) {
        this.requires = requires;
    }

    public List<ModuleConfigField> getConfigFields() {
        return configFields;
    }

    public abstract BackbonePipelineComponent<?, ?> getInstance(PipelineComponentDeclaration callingComponentDec, boolean loadConfig);
}

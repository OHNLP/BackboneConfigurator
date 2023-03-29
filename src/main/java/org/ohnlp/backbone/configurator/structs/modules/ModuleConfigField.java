package org.ohnlp.backbone.configurator.structs.modules;


import org.ohnlp.backbone.configurator.structs.modules.types.TypedConfigurationField;

public class ModuleConfigField implements Cloneable {
    private String path;
    private String desc;
    private boolean required;

    private TypedConfigurationField impl;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public TypedConfigurationField getImpl() {
        return impl;
    }

    public void setImpl(TypedConfigurationField impl) {
        this.impl = impl;
    }

    @Override
    public ModuleConfigField clone() {
        try {
            ModuleConfigField clone = (ModuleConfigField) super.clone();
            clone.path = path;
            clone.desc = desc;
            clone.required = required;
            clone.impl = impl.clone();
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}

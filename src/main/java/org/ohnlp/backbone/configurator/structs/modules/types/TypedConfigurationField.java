package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class TypedConfigurationField implements Cloneable {

    private Object currValue;

    public void setCurrValue(Object currValue) {
        this.currValue = currValue;
    }

    public Object getCurrValue() {
        return this.currValue;
    }

    public abstract void injectValueFromJSON(JsonNode json);

    public abstract JsonNode valueToJSON();

    @Override
    public TypedConfigurationField clone() {
        ObjectMapper om = new ObjectMapper();
        try {
            TypedConfigurationField clone = (TypedConfigurationField) super.clone();
            clone.currValue = om.readTree(om.writer().writeValueAsString(this.currValue));
            cloneFields(clone);
            return clone;
        } catch (CloneNotSupportedException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public abstract void cloneFields(TypedConfigurationField target);
}
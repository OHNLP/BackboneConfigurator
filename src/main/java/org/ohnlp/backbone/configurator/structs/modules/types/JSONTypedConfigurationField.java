package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;

public class JSONTypedConfigurationField extends TypedConfigurationField {
    private boolean object;
    private boolean array;

    public JSONTypedConfigurationField(boolean explicitlyObject, boolean explicityArray) {
        this.object = explicitlyObject;
        this.array = explicityArray;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        this.setCurrValue(json);
    }

    @Override
    public JsonNode valueToJSON() {
        if (this.getCurrValue() != null) {
            return (JsonNode) this.getCurrValue();
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((JSONTypedConfigurationField) target).object = object;
        ((JSONTypedConfigurationField) target).array = array;
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class InputColumnTypedConfigurationField extends TypedConfigurationField {
    private boolean isColumnList;

    public InputColumnTypedConfigurationField(boolean isColumnList) {
        this.isColumnList = isColumnList;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isTextual()) {
            this.setCurrValue(json.asText());
        }
        // Otherwise invalid value/discard TODO error/warn
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            return JsonNodeFactory.instance.textNode(getCurrValue().toString());
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((InputColumnTypedConfigurationField)target).isColumnList = isColumnList;
    }
}

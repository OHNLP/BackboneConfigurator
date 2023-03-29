package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

public class BooleanTypedConfigurationField extends TypedConfigurationField {
    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isBoolean()) {
            this.setCurrValue(json.asBoolean());
        }
        // Otherwise invalid value/discard TODO error/warn
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            return JsonNodeFactory.instance.booleanNode((Boolean) getCurrValue());
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {

    }
}

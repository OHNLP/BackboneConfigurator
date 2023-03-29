package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashMap;
import java.util.Map;

public class MapTypedConfigurationField extends TypedConfigurationField {
    private TypedConfigurationField key;
    private TypedConfigurationField value;

    public MapTypedConfigurationField(TypedConfigurationField key, TypedConfigurationField value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isObject()) {
            Map<TypedConfigurationField, TypedConfigurationField> valueMap = new HashMap<>();
            json.fields().forEachRemaining((e) -> {
                JsonNode key = JsonNodeFactory.instance.textNode(e.getKey());
                JsonNode child = e.getValue();
                TypedConfigurationField keyField = this.key.clone();
                keyField.injectValueFromJSON(key);
                if (keyField.getCurrValue() != null) {
                    TypedConfigurationField valField = this.value.clone();
                    valField.injectValueFromJSON(child);
                    valueMap.put(keyField, valField);
                    return;
                }
                // Invalid value TODO

            });
            this.setCurrValue(valueMap);
        }
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            ObjectNode ret = JsonNodeFactory.instance.objectNode();
            ((Map<TypedConfigurationField, TypedConfigurationField>) getCurrValue()).forEach((field, child) -> {
                JsonNode childNode = child.valueToJSON();
                if (childNode != null) {
                    ret.set(field.valueToJSON().asText(), childNode);
                }
            });
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((MapTypedConfigurationField)target).key = key.clone();
        ((MapTypedConfigurationField)target).value = value.clone();
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.util.LinkedHashSet;
import java.util.List;

public class EnumerationTypedConfigurationField extends TypedConfigurationField {
    private LinkedHashSet<String> constants;

    public EnumerationTypedConfigurationField(List<String> constants) {
        this.constants = new LinkedHashSet<>(constants);
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (this.constants.contains(json.asText())) {
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
        ((EnumerationTypedConfigurationField) target).constants = new LinkedHashSet<>(constants);
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.util.SchemaConfigUtils;

public class SchemaTypedConfigurationField extends TypedConfigurationField {
    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isObject()) {
            try {
                Schema s = SchemaConfigUtils.jsonToSchema(json);
                this.setCurrValue(s);
            } catch (Throwable ignored) {}
        }
        // invalid input TODO
    }

    @Override
    public JsonNode valueToJSON() {
        if (this.getCurrValue() != null) {
            return SchemaConfigUtils.schemaToJSON((Schema) this.getCurrValue());
        }
        return null; // TODO
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {

    }
}

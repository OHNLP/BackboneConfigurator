package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.util.SchemaConfigUtils;

import java.util.List;

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

    @Override
    public Node render(List<InputColumn> availableColumns) { // TODO
        TextField ret = new TextField();
        ret.textProperty().bind(observableEditedValue.asString());
        return ret;
    }
}

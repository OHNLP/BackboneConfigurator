package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

import java.util.Map;

public class StringTypedConfigurationField extends TypedConfigurationField {
    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isTextual()) {
            this.setCurrValue(json.asText());
        }
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
    }

    @Override
    public Node render(Map<String, Schema> schema) {
        TextField ret = new TextField(observableEditedValue.asString().get());
        ret.textProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        return ret;
    }
}

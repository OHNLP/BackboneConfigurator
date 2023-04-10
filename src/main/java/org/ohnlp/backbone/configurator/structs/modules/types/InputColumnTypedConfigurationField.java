package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class InputColumnTypedConfigurationField extends TypedConfigurationField {
    public InputColumnTypedConfigurationField() {

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
    }

    @Override
    public Node render() { // TODO
        TextField ret = new TextField(observableEditedValue.asString().get());
        ret.textProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        return ret;
    }
}

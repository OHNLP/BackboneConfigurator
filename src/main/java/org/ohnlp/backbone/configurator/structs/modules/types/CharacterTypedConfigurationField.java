package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

public class CharacterTypedConfigurationField extends TypedConfigurationField {
    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isTextual()) {
            if (json.asText().length() > 0) {
                setCurrValue(json.asText().toCharArray()[0]);
            }
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
    public Node render(ObservableMap<String, Schema> schema) {
        TextField ret = new TextField(observableEditedValue.asString().get());
        ret.textProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        return ret;
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import org.apache.beam.sdk.schemas.Schema;

import java.util.Map;

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

    @Override
    public Node render(Map<String, Schema> schema) {
        HBox ret = new HBox();
        ret.setAlignment(Pos.CENTER_LEFT);
        ret.setSpacing(5);
        final ToggleGroup g = new ToggleGroup();
        boolean selected = observableEditedValue.isEqualTo(true).get();
        RadioButton t = new RadioButton("True");
        t.setSelected(selected);
        RadioButton f = new RadioButton("False");
        f.setSelected(!selected);
        t.setToggleGroup(g);
        f.setToggleGroup(g);
        t.selectedProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        ret.getChildren().addAll(t, f);
        return ret;
    }
}

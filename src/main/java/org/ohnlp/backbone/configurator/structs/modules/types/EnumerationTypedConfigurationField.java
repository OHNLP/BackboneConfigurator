package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import org.apache.beam.sdk.schemas.Schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

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

    @Override
    public Node render(Map<String, Schema> schema) {
        ComboBox<String> ret = new ComboBox<>();
        ObservableList<String> items = FXCollections.observableArrayList(constants);
        ret.setItems(items);
        if (this.observableEditedValue.get() != null) {
            int idx = new ArrayList<>(constants).indexOf(this.observableEditedValue.get());
            ret.getSelectionModel().select(idx);
        }
        ret.getSelectionModel().selectedItemProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        ret.setMaxWidth(Double.MAX_VALUE);
        return ret;
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
    public Node render(List<InputColumn> availableColumns) { // TODO split sourceTag and columnname into two combo boxes
        ComboBox<String> ret = new ComboBox<>();
        ret.setEditable(true);
        List<String> cols = availableColumns.stream().map(s -> s.sourceTag + "." + s.columnName).collect(Collectors.toList());
        ObservableList<String> items = FXCollections.observableArrayList(cols);
        ret.setItems(items);
        if (this.observableEditedValue.get() != null) {
            int idx = new ArrayList<>(cols).indexOf(this.observableEditedValue.get().toString());
            ret.getSelectionModel().select(idx);
            ret.setValue(this.observableEditedValue.get().toString());
        }
        ret.valueProperty().addListener((obs, ov, nv) -> {
            ret.setValue(nv);
            this.updateValue(nv);
        });
        return ret;
    }
}

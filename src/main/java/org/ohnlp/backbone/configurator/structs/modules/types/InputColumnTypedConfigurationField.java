package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import org.apache.beam.sdk.schemas.Schema;

import java.util.Map;
import java.util.Optional;

public class InputColumnTypedConfigurationField extends TypedConfigurationField {
    private Map<String, Schema> schema;

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
    public Node render(ObservableMap<String, Schema> schema) {
        HBox ret = new HBox();
        ret.setSpacing(5);
        ret.setAlignment(Pos.CENTER_LEFT);
        this.schema = schema;
        // Source Collection Schemas
        ComboBox<String> sourceComponent = new ComboBox<>();
        sourceComponent.setEditable(true);
        ObservableList<String> items = FXCollections.observableArrayList();
        items.addAll(schema.keySet());
        schema.addListener((MapChangeListener<String, Schema>) change -> {
            items.clear();
            items.addAll(change.getMap().keySet());
        });
        sourceComponent.setItems(items.sorted());
        // Fields for a given Source Schema
        ComboBox<String> sourceFieldName = new ComboBox<>();
        sourceFieldName.setEditable(true);
        ObservableList<String> sourceFieldItems = FXCollections.observableArrayList();
        sourceFieldName.setItems(sourceFieldItems);

        // Actually populate the return HBox and set bound growth
        ret.getChildren().addAll(new Text("Input Collection:"), sourceComponent, new Text("Input Field Name:"), sourceFieldName);
        sourceComponent.setMaxWidth(Double.MAX_VALUE);
        sourceFieldName.setMaxWidth(Double.MAX_VALUE);

        // Populate model if existing
        if (this.observableEditedValue.get() != null) {
            String raw = this.observableEditedValue.get().toString();
            int sepIdx = raw.indexOf(".");
            String srcTag = "";
            String fieldName = raw;
            if (sepIdx != -1) {
                srcTag = raw.substring(0, sepIdx);
                fieldName = raw.substring(sepIdx + 1);
            }
            int srcTagIdx = sourceComponent.getItems().indexOf(srcTag);
            sourceComponent.getSelectionModel().select(srcTagIdx);
            sourceComponent.setValue(srcTag);
            int srcFieldIdx = sourceFieldName.getItems().indexOf(fieldName);
            sourceFieldName.getSelectionModel().select(srcFieldIdx);
            sourceFieldName.setValue(fieldName);
        }

        // Bind functionality
        // - Bind updating source field list to changes in sourceComponent selection
        sourceComponent.valueProperty().addListener((o, ov, nv) -> {
            sourceFieldName.getItems().clear();
            Schema target = schema.get(nv);
            if (target != null) {
                sourceFieldName.getItems().addAll(target.getFieldNames());
            }
        });
        schema.addListener((MapChangeListener<String, Schema>) change -> {
            if (sourceComponent.getValue() != null) {
                if (sourceComponent.getValue().equals(change.getKey())) {
                    sourceFieldItems.clear();
                    sourceFieldItems.addAll(change.getValueAdded().getFieldNames());
                }
            }
        });
        // - Bind changes to sourcecomponent or sourcefieldname values to updateValue
        sourceComponent.valueProperty().addListener((e, o, n) -> {
            generateAndUpdateValue(sourceComponent, sourceFieldName);
        });
        sourceFieldName.valueProperty().addListener((e, o, n) -> {
            generateAndUpdateValue(sourceComponent, sourceFieldName);
        });
        return ret;
    }

    private void generateAndUpdateValue(ComboBox<String> sourceComponent, ComboBox<String> sourceFieldName) {
        String component = Optional.ofNullable(sourceComponent.getValue()).orElse("").trim();
        String fieldName = Optional.ofNullable(sourceFieldName.getValue()).orElse("").trim();
        if (fieldName.length() > 0) {
            if (component.length() > 0) {
                updateValue(component + "." + fieldName);
            } else {
                updateValue(fieldName);
            }
        } else {
            updateValue(null);
        }

    }
}

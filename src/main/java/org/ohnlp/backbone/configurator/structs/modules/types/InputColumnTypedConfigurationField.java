package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.util.Callback;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.annotations.InputColumnProperty;
import org.ohnlp.backbone.api.config.InputColumn;

import java.util.*;

public class InputColumnTypedConfigurationField extends TypedConfigurationField {
    private final Set<String> types;
    private final Set<String> colls;
    ;
    private Map<String, Schema> schema;

    public InputColumnTypedConfigurationField() {
        this.types = new HashSet<>();
        this.colls = new HashSet<>();
    }

    public InputColumnTypedConfigurationField(InputColumnProperty prop) {
        this.types = new HashSet<>(Arrays.asList(prop.allowableTypes()));
        this.colls = new HashSet<>(Arrays.asList(prop.sourceTags()));
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isTextual() || json.isObject()) {
            try {
                this.setCurrValue(new ObjectMapper().treeToValue(json, InputColumn.class));
            } catch (JsonProcessingException ignore) { // Invalid value/discard TODO error/warn
            }
        }
        // Otherwise invalid value/discard TODO error/warn
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            ObjectNode ret = JsonNodeFactory.instance.objectNode();
            ret.put("sourceColumnName", ((InputColumn)getCurrValue()).getSourceColumnName());
            ret.put("sourceTag", ((InputColumn)getCurrValue()).getSourceTag());
            return ret;
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
        if (colls.size() > 0) {
            sourceComponent.setItems(sourceComponent.getItems().filtered(colls::contains));
        }
        // Fields for a given Source Schema
        ComboBox<String> sourceFieldName = new ComboBox<>();
        sourceFieldName.setEditable(true);
        ObservableList<String> sourceFieldItems = FXCollections.observableArrayList();
        sourceFieldName.setItems(sourceFieldItems);

        // Actually populate the return HBox and set bound growth
        // - Only render source collection seelction if there is more than 1 element
        boolean renderSource = sourceComponent.getItems().size() > 1;
        if (renderSource) {
            ret.getChildren().addAll(new Text("Input Collection:"), sourceComponent);
        }
        ret.getChildren().addAll(new Text("Input Field Name:"), sourceFieldName);
        sourceComponent.setMaxWidth(Double.MAX_VALUE);
        sourceFieldName.setMaxWidth(Double.MAX_VALUE);

        // Populate model if existing
        if (this.observableEditedValue.get() != null) {
            InputColumn raw = (InputColumn) this.observableEditedValue.get();
            String srcTag = raw.getSourceTag();
            // - Force update to correct tag if only one input
            if (!renderSource) {
                srcTag = schema.keySet().stream().findFirst().orElse("*");
            }
            String fieldName = raw.getSourceColumnName();
            int srcTagIdx = sourceComponent.getItems().indexOf(srcTag);
            sourceComponent.getSelectionModel().select(srcTagIdx);
            sourceComponent.setValue(srcTag);
            sourceFieldItems.clear();
            if (schema.containsKey(srcTag)) {
                sourceFieldItems.addAll(schema.get(srcTag).getFieldNames());
            }
            int srcFieldIdx = sourceFieldName.getItems().indexOf(fieldName);
            sourceFieldName.getSelectionModel().select(srcFieldIdx);
            sourceFieldName.setValue(fieldName);
        }
        // Disable input column if unresolvable/autogenerated
        sourceFieldName.setCellFactory(new Callback<>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);

                        if (item != null || !empty) {
                            this.setText(item);
                            this.setDisable(item.contains("_unresolvable_fields"));
                        }
                    }
                };
            }
        });
        sourceFieldName.valueProperty().addListener((e,o,n) -> {
            if (n != null) {
                if (n.contains("_unresolvable_fields")) {
                    sourceFieldName.setValue(null);
                    sourceFieldName.getSelectionModel().clearSelection();
                }
            }
        });


        // Bind functionality
        // - Bind updating source field list to changes in sourceComponent selection
        if (renderSource) {
            sourceComponent.valueProperty().addListener((o, ov, nv) -> {
                sourceFieldName.getItems().clear();
                Schema target = schema.get(nv);
                if (target != null) {
                    sourceFieldName.getItems().addAll(target.getFieldNames());
                }
            });
        } else {
            sourceComponent.valueProperty().set(schema.keySet().stream().findFirst().orElse("*"));
        }
        schema.addListener((MapChangeListener<String, Schema>) change -> {
            if (sourceComponent.getValue() != null) {
                if (sourceComponent.getValue().equals(change.getKey())) {
                    sourceFieldItems.clear();
                    sourceFieldItems.addAll(change.getValueAdded().getFieldNames());
                }
            }
        });
        // - Bind changes to sourcecomponent or sourcefieldname values to updateValue
        if (renderSource) {
            sourceComponent.valueProperty().addListener((e, o, n) -> {
                generateAndUpdateValue(sourceComponent, sourceFieldName);
            });
        }
        sourceFieldName.valueProperty().addListener((e, o, n) -> {
            generateAndUpdateValue(sourceComponent, sourceFieldName);
        });

        return ret;
    }

    private void generateAndUpdateValue(ComboBox<String> sourceComponent, ComboBox<String> sourceFieldName) {
        String component = Optional.ofNullable(sourceComponent.getValue()).orElse("").trim();
        String fieldName = Optional.ofNullable(sourceFieldName.getValue()).orElse("").trim();
        if (fieldName.length() > 0) {
            if (component.length() == 0) {
                component = "*";
            }
            InputColumn ret = new InputColumn();
            ret.setSourceTag(component);
            ret.setSourceColumnName(fieldName);
            updateValue(ret);
        } else {
            updateValue(null);
        }
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.util.SchemaConfigUtils;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

public class SchemaTypedConfigurationField extends TypedConfigurationField {

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isObject()) {
            try {
                Schema s = SchemaConfigUtils.jsonToSchema(json);
                this.setCurrValue(s);
            } catch (Throwable ignored) {
            }
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
    public Node render(Map<String, Schema> schema) {
        SimpleBooleanProperty invalidationFlag = new SimpleBooleanProperty(false); // Use this to track changes to if contents change/need to be updated
        SchemaEditorNode ret = new ObjectSchemaEditorNode(invalidationFlag, null);
        // Add listener to invalidationFlag to update values on change to true
        invalidationFlag.addListener((e, o, n) -> {
            if (n) {
                ObjectNode updated = JsonNodeFactory.instance.objectNode();
                ret.toJSON(updated);
                updateValue(ret);
                invalidationFlag.set(false);
            }
        });
        // Now load values if existing
        if (this.observableEditedValue.isNotNull().get()) {
            ret.loadFromJSON((ObjectNode) this.observableEditedValue.get());
        }
        return ret.render();
    }

    private static class SchemaEditorNode {
        protected final SimpleBooleanProperty invalidationFlag;
        protected ObservableList<SchemaEditorNode> parent;
        protected TextField fieldName;
        protected ComboBox<String> editor;
        protected SimpleObjectProperty<SchemaEditorNode> child;
        public static String[] BEAM_TYPES = {"STRING", "BYTE", "BYTES", "INT16", "INT32", "INT64", "FLOAT", "DOUBLE", "DECIMAL", "BOOLEAN", "DATETIME", "COLLECTION", "OBJECT"};

        public SchemaEditorNode(SimpleBooleanProperty invalidationFlag, ObservableList<SchemaEditorNode> parent) {
            this.parent = parent;
            this.fieldName = new TextField("fieldName");
            this.child = new SimpleObjectProperty<>();
            this.editor = new ComboBox<>(FXCollections.observableArrayList(BEAM_TYPES));
            this.editor.setValue("STRING");
            // Bind to determine appropriate children to render
            this.editor.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
                if (!Arrays.asList("COLLECTION", "OBJECT").contains(newVal)) {
                    this.child.set(null);
                } else if (newVal.equals("COLLECTION")) {
                    this.child.set(new CollectionSchemaEditorNode(invalidationFlag, null));
                } else {
                    this.child.set(new ObjectSchemaEditorNode(invalidationFlag, null));
                }
            });
            // Bind to invalidate if values change
            this.fieldName.textProperty().addListener((e, o, n) -> {
                invalidationFlag.set(true);
            });
            this.editor.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
                invalidationFlag.set(true);
            });
            this.invalidationFlag = invalidationFlag;
        }

        public Node render() {
            HBox ret = new HBox();
            ret.getChildren().addAll(fieldName, editor, new Pane());
            if (this.parent != null) {
                Button remove = new Button("-");
                remove.setOnMouseClicked(e -> {
                    if (e.getButton().equals(MouseButton.PRIMARY)) {
                        this.parent.remove(this);
                    }
                });
                ret.getChildren().add(remove);
            }
            // Add listener to change child render if this node has children
            this.child.isNotNull().addListener((e, ov, nv) -> {
                if (nv) {
                    ret.getChildren().set(2, this.child.get().render());
                } else {
                    ret.getChildren().set(2, new Pane());
                }
                HBox.setHgrow(ret.getChildren().get(2), Priority.ALWAYS);
            });
            return ret;
        }

        public void toJSON(JsonNode parent) {
            if (!parent.isObject()) {
                throw new IllegalArgumentException("Attempting to populate fields in a non-object type");
            }
            String fieldName = this.fieldName.textProperty().getValue();
            String type = this.editor.getSelectionModel().getSelectedItem();
            JsonNode val;
            if (type.equals("OBJECT")) {
                val = JsonNodeFactory.instance.objectNode();
                child.getValue().toJSON(val);
            } else if (type.equals("COLLECTION")) {
                val = JsonNodeFactory.instance.arrayNode();
                child.getValue().toJSON(val);
            } else {
                val = JsonNodeFactory.instance.textNode(type);
            }
            ((ObjectNode) parent).set(fieldName, val);
        }

        public void loadFromJSON(JsonNode json) {

            Map.Entry<String, JsonNode> e = json.fields().next();
            this.fieldName.textProperty().set(e.getKey());
            JsonNode val = e.getValue();
            if (val.isArray()) {
                this.child.set(new CollectionSchemaEditorNode(invalidationFlag, null));
                this.child.get().loadFromJSON(val);
                this.editor.getSelectionModel().select("COLLECTION");
            } else if (val.isObject()) {
                this.child.set(new ObjectSchemaEditorNode(invalidationFlag, null));
                this.editor.getSelectionModel().select("OBJECT");
            } else {
                this.child.set(null);
                this.editor.getSelectionModel().select(val.asText().toUpperCase(Locale.ROOT));
            }
        }
    }

    private static class CollectionSchemaEditorNode extends SchemaEditorNode {
        private HBox render;

        public CollectionSchemaEditorNode(SimpleBooleanProperty invalidationFlag, ObservableList<SchemaEditorNode> parent) {
            super(invalidationFlag, parent);
            this.child.set(null);
            this.render = new HBox(new Text("Content Type:"), this.editor, new Pane());
            this.render.setSpacing(5);
            this.render.setAlignment(Pos.CENTER_LEFT);
            this.child.addListener((e, ov, nv) -> {
                if (nv != null) {
                    Node n = nv.render();
                    render.getChildren().set(2, n);
                    HBox.setHgrow(n, Priority.ALWAYS);
                } else {
                    render.getChildren().set(2, new Pane());
                }
                invalidationFlag.set(true);
            });
        }

        @Override
        public Node render() {
            return render;
        }

        public void toJSON(JsonNode parent) {
            if (!parent.isArray()) {
                throw new IllegalArgumentException("Tried to add array element type to non-array");
            }
            if (this.child.isNotNull().get()) {
                if (this.child.get() instanceof CollectionSchemaEditorNode) {
                    ArrayNode toAdd = JsonNodeFactory.instance.arrayNode();
                    this.child.get().toJSON(toAdd);
                    ((ArrayNode) parent).add(toAdd);
                } else {
                    ObjectNode toAdd = JsonNodeFactory.instance.objectNode();
                    this.child.get().toJSON(toAdd);
                    ((ArrayNode) parent).add(toAdd);
                }
            }
        }

        @Override
        public void loadFromJSON(JsonNode json) {
            if (!json.isArray()) {
                throw new IllegalArgumentException("Trying to load a collection from a non-collection type");
            }
            ArrayNode arr = (ArrayNode) json;
            if (arr.size() == 0) {
                this.child.set(null);
            } else {
                if (arr.get(0).isArray()) {
                    CollectionSchemaEditorNode val = new CollectionSchemaEditorNode(invalidationFlag, null);
                    val.loadFromJSON(arr.get(0));
                    this.child.set(val);
                } else if (arr.get(0).isObject()) {
                    ObjectSchemaEditorNode val = new ObjectSchemaEditorNode(invalidationFlag, null);
                    val.loadFromJSON(arr.get(0));
                    this.child.set(val);
                } else {
                    this.editor.getSelectionModel().select(arr.get(0).asText().toUpperCase(Locale.ROOT));
                }
            }
        }
    }

    private static class ObjectSchemaEditorNode extends SchemaEditorNode {
        private final Button addFieldButton;
        private VBox render;
        private ObservableList<SchemaEditorNode> children;

        public ObjectSchemaEditorNode(SimpleBooleanProperty invalidationFlag, ObservableList<SchemaEditorNode> schemaEditorNode) {
            super(invalidationFlag, schemaEditorNode);
            this.children = FXCollections.observableArrayList();
            this.render = new VBox();
            this.addFieldButton = new Button("Add New Field");
            this.addFieldButton.setOnMouseClicked(e -> {
                if (e.getButton().equals(MouseButton.PRIMARY)) {
                    children.add(new SchemaEditorNode(invalidationFlag, children));
                }
            });
            this.render.getChildren().add(this.addFieldButton);
            this.children.addListener((ListChangeListener<SchemaEditorNode>) c -> {
                render.getChildren().clear();
                c.getList().forEach(node -> render.getChildren().add(node.render()));
                render.getChildren().add(addFieldButton);
            });
            // Bind to invalidate if values change
            this.children.addListener((ListChangeListener<SchemaEditorNode>) c -> invalidationFlag.set(true));
        }

        @Override
        public Node render() {
            return render;
        }

        public void loadFromJSON(JsonNode json) {
            json.fields().forEachRemaining(e -> {
                SchemaEditorNode toAdd = new SchemaEditorNode(invalidationFlag, children);
                toAdd.loadFromJSON(JsonNodeFactory.instance.objectNode().set(e.getKey(), e.getValue()));
                this.children.add(toAdd);
            });
        }
    }
}

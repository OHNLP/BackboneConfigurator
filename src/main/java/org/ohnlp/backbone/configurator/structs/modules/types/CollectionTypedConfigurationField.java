package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.apache.beam.sdk.schemas.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CollectionTypedConfigurationField extends TypedConfigurationField {

    public CollectionTypedConfigurationField() {
        setCurrValue(new ArrayList<TypedConfigurationField>());
    }
    private TypedConfigurationField contents;

    public TypedConfigurationField getContents() {
        return contents;
    }

    public void setContents(TypedConfigurationField contents) {
        this.contents = contents;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        List<TypedConfigurationField> children = new ArrayList<>();
        if (json.isArray()) {
            for (JsonNode child : json) {
                TypedConfigurationField childNode = this.contents.clone();
                childNode.injectValueFromJSON(child);
                children.add(childNode);
            }
        }
        this.setCurrValue(children);
        // Otherwise illegal/not an array, discard contents TODO error/warn
    }

    @Override
    public JsonNode valueToJSON() {
        if (this.getCurrValue() != null) {
            final ArrayNode out = JsonNodeFactory.instance.arrayNode();
            ((List<TypedConfigurationField>)this.getCurrValue()).forEach(f -> {
                JsonNode child = f.valueToJSON();
                if (child != null) {
                    out.add(child);
                }
            });
            return out;
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((CollectionTypedConfigurationField)target).contents = contents.clone();
    }

    @Override
    public Node render(Map<String, Schema> schema) {
        VBox ret = new VBox();
        Button addButton = new Button("Add new Entry");
        addButton.setOnMouseClicked((e) -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                List<TypedConfigurationField> children;
                if (this.observableEditedValue.isNotNull().get()) {
                    children = new ArrayList<>((List<TypedConfigurationField>) this.observableEditedValue.get());
                } else {
                    children = new ArrayList<>();
                }
                children.add(contents.clone());
                this.updateValue(children);
            }
        });
        ret.getChildren().clear();
        if (this.getCurrValue() != null) {
            List<TypedConfigurationField> children = (List<TypedConfigurationField>) this.getCurrValue();
            children.forEach((child) -> {
                HBox toAdd = new HBox();
                Node childRender = child.render(schema);
                Button removeChild = new Button("-");
                removeChild.setOnMouseClicked(e -> {
                    if (e.getButton().equals(MouseButton.PRIMARY)) {
                        ArrayList<TypedConfigurationField> val = new ArrayList<>((List<TypedConfigurationField>) this.observableEditedValue.get());
                        val.remove(child);
                        updateValue(val);
                    }
                });
                toAdd.getChildren().addAll(childRender, removeChild);
                HBox.setHgrow(childRender, Priority.ALWAYS);
                ret.getChildren().add(toAdd);
            });
            ret.getChildren().add(addButton);
        }
        this.observableEditedValue.addListener((observable, oldValue, newValue) -> {
            List<TypedConfigurationField> val = (List<TypedConfigurationField>) newValue;
            ret.getChildren().clear();
            val.forEach(field -> {
                HBox toAdd = new HBox();
                Node childRender = field.render(schema);
                Button removeChild = new Button("-");
                removeChild.setOnMouseClicked(e -> {
                    if (e.getButton().equals(MouseButton.PRIMARY)) {
                        ArrayList<TypedConfigurationField> v = new ArrayList<>((List<TypedConfigurationField>) this.observableEditedValue.get());
                        v.remove(field);
                        updateValue(v);
                    }
                });
                toAdd.getChildren().addAll(childRender, removeChild);
                HBox.setHgrow(childRender, Priority.ALWAYS);
                ret.getChildren().add(toAdd);
            });
            ret.getChildren().add(addButton);
        });
        return ret;
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CollectionTypedConfigurationField extends TypedConfigurationField {
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
    public Node render(List<InputColumn> availableColumns) {
        VBox ret = new VBox();
        Button addButton = new Button("Add new Entry");
        addButton.setOnMouseClicked((e) -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                List<TypedConfigurationField> children;
                if (this.getCurrValue() != null) {
                    children = (List<TypedConfigurationField>) this.getCurrValue();
                } else {
                    children = new ArrayList<>();
                }
                children.add(contents.clone());
            }
        });
        ret.getChildren().clear();
        if (this.getCurrValue() != null) {
            List<TypedConfigurationField> children = (List<TypedConfigurationField>) this.getCurrValue();
            children.forEach((child) -> {
                HBox toAdd = new HBox();
                Node childRender = child.render(availableColumns);
                Button removeChild = new Button("-");
                // TODO add listener func
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
                Node childRender = field.render(availableColumns);
                Button removeChild = new Button("-");
                // TODO add listener func
                toAdd.getChildren().addAll(childRender, removeChild);
                HBox.setHgrow(childRender, Priority.ALWAYS);
                ret.getChildren().add(toAdd);
            });
            ret.getChildren().add(addButton);
        });
        // TODO Bindings are not properly handled here

        return ret;
    }
}

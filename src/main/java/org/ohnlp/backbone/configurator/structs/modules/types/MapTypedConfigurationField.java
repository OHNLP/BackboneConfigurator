package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

import java.util.*;

public class MapTypedConfigurationField extends TypedConfigurationField {
    private TypedConfigurationField key;
    private TypedConfigurationField value;

    public MapTypedConfigurationField(TypedConfigurationField key, TypedConfigurationField value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isObject()) {
            Map<TypedConfigurationField, TypedConfigurationField> valueMap = new HashMap<>();
            json.fields().forEachRemaining((e) -> {
                JsonNode key = JsonNodeFactory.instance.textNode(e.getKey());
                JsonNode child = e.getValue();
                TypedConfigurationField keyField = this.key.clone();
                keyField.injectValueFromJSON(key);
                if (keyField.getCurrValue() != null) {
                    TypedConfigurationField valField = this.value.clone();
                    valField.injectValueFromJSON(child);
                    valueMap.put(keyField, valField);
                    return;
                }
                // Invalid value TODO

            });
            this.setCurrValue(valueMap);
        }
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            ObjectNode ret = JsonNodeFactory.instance.objectNode();
            ((Map<TypedConfigurationField, TypedConfigurationField>) getCurrValue()).forEach((field, child) -> {
                JsonNode childNode = child.valueToJSON();
                if (childNode != null) {
                    ret.set(field.valueToJSON().asText(), childNode);
                }
            });
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void loadFromDefault(Object object) {
        Map<?, ?> map = (Map<?, ?>) object;
        Map<TypedConfigurationField, TypedConfigurationField> ret = new HashMap<>();
        map.forEach((k, v) -> {
            TypedConfigurationField keyCln = this.key.clone();
            TypedConfigurationField valCln = this.value.clone();
            if (k != null) {
                keyCln.loadFromDefault(k);
            }
            if (v != null) {
                valCln.loadFromDefault(v);
            }
            ret.put(keyCln, valCln);
        });
        setCurrValue(ret);
    }


    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((MapTypedConfigurationField)target).key = key.clone();
        ((MapTypedConfigurationField)target).value = value.clone();
    }

    @Override
    public Node render(ObservableMap<String, Schema> schema) { // TODO
        TextField ret = new TextField();
        ret.textProperty().bind(observableEditedValue.asString());
        return ret;
    }
}

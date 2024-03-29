package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class ObjectTypedConfigurationField extends TypedConfigurationField {
    private Map<String, TypedConfigurationField> fields;

    public ObjectTypedConfigurationField(Map<String, TypedConfigurationField> fields) {
        this.fields = fields;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isObject()) {
            Map<String, TypedConfigurationField> valueMap = new HashMap<>();
            this.fields.forEach((field, child) -> {
                if (json.hasNonNull(field)) {
                    TypedConfigurationField childValue = child.clone();
                    childValue.injectValueFromJSON(json.get(field));
                    valueMap.put(field, childValue);
                }
            });
            this.setCurrValue(valueMap);
        }
        // Invalid Value TODO
    }

    @Override
    public JsonNode valueToJSON() {
        if (getCurrValue() != null) {
            ObjectNode ret = JsonNodeFactory.instance.objectNode();
            ((Map<String, TypedConfigurationField>) getCurrValue()).forEach((field, child) -> {
                JsonNode childNode = child.valueToJSON();
                if (childNode != null) {
                    ret.set(field, childNode);
                }
            });
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public void loadFromDefault(Object object) {

        Map<String, TypedConfigurationField> ret = new HashMap<>();
        fields.forEach((k, v) -> ret.put(k, v.clone()));
        fields.keySet().forEach(field -> {
            try {
                Field f = object.getClass().getDeclaredField(field);
                f.trySetAccessible();
                Object o = f.get(object);
                if (o != null) {
                    ret.get(field).loadFromDefault(o);
                }
            } catch (Throwable ignored) {
            }
        });
        setCurrValue(ret);
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        Map<String, TypedConfigurationField> tgtFields = new HashMap<>();
        fields.forEach((k,v) -> tgtFields.put(k, v.clone()));
        ((ObjectTypedConfigurationField)target).fields = tgtFields;
    }

    @Override
    public Node render(ObservableMap<String, Schema> schema) { // TODO
        TextField ret = new TextField();
        ret.textProperty().bind(observableEditedValue.asString());
        return ret;
    }

}

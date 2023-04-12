package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import org.apache.beam.sdk.schemas.Schema;

import java.util.List;
import java.util.Map;

public abstract class TypedConfigurationField implements Cloneable {

    private Object currValue;
    protected SimpleObjectProperty<Object> observableEditedValue = new SimpleObjectProperty<>();


    public void setCurrValue(Object currValue) {
        this.currValue = currValue;
        this.observableEditedValue.set(currValue);
    }

    public void updateValue(Object value) {
        this.observableEditedValue.setValue(value);
    }

    public Object getCurrValue() {
        return this.currValue;
    }

    public abstract void injectValueFromJSON(JsonNode json);

    public abstract JsonNode valueToJSON();

    @Override
    public TypedConfigurationField clone() {
        ObjectMapper om = new ObjectMapper();
        try {
            TypedConfigurationField clone = (TypedConfigurationField) super.clone();
            if (this.currValue != null) {
                clone.currValue = om.readValue(om.writer().writeValueAsString(this.currValue), this.currValue.getClass());

            }
            clone.observableEditedValue = new SimpleObjectProperty<>();
            if (this.currValue != null) {
                clone.observableEditedValue.setValue(om.readValue(om.writeValueAsString(this.currValue), this.currValue.getClass()));
            }
            cloneFields(clone);
            return clone;
        } catch (CloneNotSupportedException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void commit() {
        this.currValue = this.observableEditedValue.getValue();
    }

    public void reset() {
        this.observableEditedValue.set(this.currValue);
    }

    public abstract void cloneFields(TypedConfigurationField target);

    public abstract Node render(Map<String, Schema> schema);

    public boolean isDirty() {
        if (currValue == null) {
            return observableEditedValue.isNotNull().get();
        } else {
            return !currValue.equals(observableEditedValue.get());
        }
    }
}
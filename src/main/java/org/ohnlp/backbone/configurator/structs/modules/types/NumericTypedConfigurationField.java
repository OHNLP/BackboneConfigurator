package org.ohnlp.backbone.configurator.structs.modules.types;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

public class NumericTypedConfigurationField extends TypedConfigurationField {
    private boolean floating;
    private Number minValue;
    private Number maxValue;

    public NumericTypedConfigurationField(boolean floating, Number minValue, Number maxValue) {
        this.floating = floating;
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        if (json.isNull()) {
            this.setCurrValue(this.floating ? 0. : 0);
        } else {
            if (this.floating && json.isFloatingPointNumber()) {
                this.setCurrValue(json.asDouble());
            } else {
                this.setCurrValue(json.asLong());
            }
        }
    }

    @Override
    public JsonNode valueToJSON() {
        try {
            if (getCurrValue() != null) {
                if (this.floating) {
                    return JsonNodeFactory.instance.numberNode(Double.parseDouble(getCurrValue().toString()));
                } else {
                    return JsonNodeFactory.instance.numberNode(Long.parseLong(getCurrValue().toString()));
                }
            } else {
                return null;
            }
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((NumericTypedConfigurationField)target).floating = floating;
        ((NumericTypedConfigurationField)target).minValue = minValue;
        ((NumericTypedConfigurationField)target).maxValue = maxValue;
    }

    @Override
    public Node render(ObservableMap<String, Schema> schema) {

        TextField ret = new TextField(observableEditedValue.asString().get());
        ret.textProperty().addListener((obs, ov, nv) -> {
            try {
                this.updateValue(this.floating ? Double.parseDouble(nv) : Long.parseLong(nv));
            } catch (Throwable ignored) {
                this.updateValue(this.floating ? 0. : 0);
            }
        });
        return ret;
    }
}

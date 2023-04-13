package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import javafx.collections.ObservableMap;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.sdk.schemas.Schema;

public class JSONTypedConfigurationField extends TypedConfigurationField {
    private boolean object;
    private boolean array;

    public JSONTypedConfigurationField(boolean explicitlyObject, boolean explicityArray) {
        this.object = explicitlyObject;
        this.array = explicityArray;
    }

    @Override
    public void injectValueFromJSON(JsonNode json) {
        this.setCurrValue(json);
    }

    @Override
    public JsonNode valueToJSON() {
        if (this.getCurrValue() != null) {
            return (JsonNode) this.getCurrValue();
        } else {
            return null;
        }
    }

    @Override
    public void cloneFields(TypedConfigurationField target) {
        ((JSONTypedConfigurationField) target).object = object;
        ((JSONTypedConfigurationField) target).array = array;
    }

    @Override
    public Node render(ObservableMap<String, Schema> schema) { // TODO
        TextField ret = new TextField(observableEditedValue.asString().get());
        ret.textProperty().addListener((obs, ov, nv) -> {
            this.updateValue(nv);
        });
        return ret;
    }
}

package org.ohnlp.backbone.configurator.structs.modules.types;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;

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
}

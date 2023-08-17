package org.ohnlp.backbone.configurator.structs.modules.serde;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.configurator.structs.modules.ModuleConfigField;
import org.ohnlp.backbone.configurator.structs.modules.PythonModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.types.*;

import java.io.IOException;

public class PythonModulePipelineComponentDeclarationDeserializer extends StdDeserializer<PythonModulePipelineComponentDeclaration> {

    public PythonModulePipelineComponentDeclarationDeserializer() {
        this(null);
    }
    protected PythonModulePipelineComponentDeclarationDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public PythonModulePipelineComponentDeclaration deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {
        JsonNode root = jsonParser.getCodec().readTree(jsonParser);
        PythonModulePipelineComponentDeclaration ret = new PythonModulePipelineComponentDeclaration();
        ret.setName(root.get("name").asText());
        ret.setDesc(root.get("desc").asText());
        // Bundle Name needs to be set at a bundle level instead of within component deserialization
        ret.setEntry_point(root.get("entry_point").asText());
        ret.setClass_name(root.get("class_name").asText());
        if (root.has("config_def")) {
            root.get("config_def").fields().forEachRemaining(e -> {
                ret.getConfigFields().add(parseJsonModuleConfigField(e.getKey(), e.getValue()));
            });
        }
        return ret;
    }

    private ModuleConfigField parseJsonModuleConfigField(String key, JsonNode fieldDefinition) {
        ModuleConfigField ret = new ModuleConfigField();
        ret.setPath(key);
        ret.setDesc(fieldDefinition.get("name").asText());
        ret.setRequired(fieldDefinition.has("required") ? fieldDefinition.get("required").asBoolean() : !fieldDefinition.has("default"));
        TypedConfigurationField impl;
        String typeName = fieldDefinition.get("type").asText();
        if (typeName.equals("COLUMNDEF")) {
            impl = new InputColumnTypedConfigurationField();
        } else {
            Schema.TypeName schemaType = Schema.TypeName.valueOf(typeName);
            impl = createConfigFieldForSchemaType(schemaType);
        }
        if (fieldDefinition.has("default")) {
            impl.injectValueFromJSON(fieldDefinition.get("default"));
        }
        ret.setImpl(impl);
        return ret;
    }

    private TypedConfigurationField createConfigFieldForSchemaType(Schema.TypeName schemaType) {
        switch (schemaType) {
            case BYTE:
                return new NumericTypedConfigurationField(false, Byte.MIN_VALUE, Byte.MAX_VALUE);
            case INT16:
                return new NumericTypedConfigurationField(false, Short.MIN_VALUE, Short.MAX_VALUE);
            case INT32:
                return new NumericTypedConfigurationField(false, Integer.MIN_VALUE, Integer.MAX_VALUE);
            case INT64:
                return new NumericTypedConfigurationField(false, Long.MIN_VALUE, Long.MAX_VALUE);
            case DECIMAL:
            case DOUBLE:
                return new NumericTypedConfigurationField(true, Double.MIN_VALUE, Double.MAX_VALUE);
            case FLOAT:
                return new NumericTypedConfigurationField(true, Float.MIN_VALUE, Float.MAX_VALUE);
            case STRING:
                return new StringTypedConfigurationField();
            case DATETIME:
                throw new IllegalArgumentException("DateTime fields not supported! Use String in ISO8601 format instead");
            case BOOLEAN:
                return new BooleanTypedConfigurationField();
            case BYTES:
            case ROW:
            case MAP:
            case ITERABLE:
            case ARRAY:
            case LOGICAL_TYPE:
            default:
                throw new IllegalStateException("Unexpected value: " + schemaType);
        }
    }
}

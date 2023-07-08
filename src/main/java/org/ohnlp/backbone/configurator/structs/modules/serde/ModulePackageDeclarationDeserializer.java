package org.ohnlp.backbone.configurator.structs.modules.serde;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.ComponentLang;
import org.ohnlp.backbone.api.annotations.ComponentDescription;
import org.ohnlp.backbone.api.annotations.ConfigurationProperty;
import org.ohnlp.backbone.api.annotations.InputColumnProperty;
import org.ohnlp.backbone.api.config.InputColumn;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.structs.modules.*;
import org.ohnlp.backbone.configurator.structs.modules.types.*;
import org.ohnlp.backbone.configurator.util.ModuleClassLoader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class ModulePackageDeclarationDeserializer extends StdDeserializer<ModulePackageDeclaration> {
    private final ObjectMapper objectMapper;

    public ModulePackageDeclarationDeserializer() {
        this(null);
    }
    public ModulePackageDeclarationDeserializer(Class<?> vc) {
        super(vc);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ModulePackageDeclaration deserialize(JsonParser jp, DeserializationContext deserializationContext) throws IOException, JacksonException {
        ModulePackageDeclaration ret = new ModulePackageDeclaration();
        JsonNode root = jp.getCodec().readTree(jp);
        ret.setName(root.get("name").asText());
        ret.setDesc(root.get("desc").asText());
        if (root.has("repo")) {
            ret.setRepo(root.get("repo").asText());
        }
        ret.setVersion(root.get("version").asText());
        if (root.has("lang")) {
            ret.setLang(ComponentLang.valueOf(root.get("lang").asText()));
        } else {
            ret.setLang(ComponentLang.JAVA);
        }
        List<String> packages = new ArrayList<>();
        ret.setPackages(packages);
        if (root.has("packages")) {
            for (JsonNode packageName : root.get("packages")) {
                packages.add(packageName.asText());
            }
        }
        if (root.has("module_identifier")) {
            ret.setModuleIdentifier(root.get("module_identifier").asText());
        }
        ret.setComponents(new ArrayList<>());
        switch (ret.getLang()) {
            case JAVA:
                scanAndLoadComponentsJava(ret, root);
                break;
            case PYTHON:
                scanAndLoadComponentsPython(ret, root);
                break;
        }
        return ret;
    }

    private void scanAndLoadComponentsJava(ModulePackageDeclaration module, JsonNode moduleDec) {
        ModuleClassLoader classloader = ModuleRegistry.getComponentClassLoader();
        ClassPathScanningCandidateComponentProvider packageScanningProvider
                = new ClassPathScanningCandidateComponentProvider(true);
        packageScanningProvider.setResourceLoader(new DefaultResourceLoader(new URLClassLoader(classloader.getURLs())));
        packageScanningProvider.addIncludeFilter(new AssignableTypeFilter(BackbonePipelineComponent.class));
        // Scan packages for annotated entities
        if (module.getPackages() != null) {
            new HashSet<>(module.getPackages()).forEach(
                    basePackage -> {
                        Set<BeanDefinition> components = packageScanningProvider.findCandidateComponents(basePackage);
                        components.forEach(component -> {
                            try {
                                Class<? extends BackbonePipelineComponent<?, ?>> clazz
                                        = (Class<? extends BackbonePipelineComponent<?, ?>>) Class.forName(component.getBeanClassName(), true, classloader);
                                if (!Modifier.isAbstract(clazz.getModifiers())) {
                                    ModulePipelineComponentDeclaration componentDec = loadJavaComponentDeclaration(clazz);
                                    module.getComponents().add(componentDec);
                                }
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        });
                    }
            );
        }
    }


    private void scanAndLoadComponentsPython(ModulePackageDeclaration module, JsonNode moduleDec) throws JsonProcessingException {
        String bundleName = moduleDec.get("module_identifier").asText();
        if (moduleDec.has("components")) {
            for (JsonNode componentDec : moduleDec.get("components")) {
                PythonModulePipelineComponentDeclaration component = this.objectMapper.treeToValue(componentDec, PythonModulePipelineComponentDeclaration.class); // Bundle Name needs to be set at a bundle level instead of within component deserialization
                component.setBundle_identifier(bundleName);
                module.getComponents().add(component);
            }
        }
    }

    /**
     * Scan the provided module's annotations and populate relevant fields in the module
     *
     * @param clazz The module class file
     */
    private ModulePipelineComponentDeclaration loadJavaComponentDeclaration(Class<? extends BackbonePipelineComponent<?, ?>> clazz) {
        ModulePipelineComponentDeclaration module = new JavaModulePipelineComponentDeclaration(clazz);
        ComponentDescription desc = clazz.getDeclaredAnnotation(ComponentDescription.class);
        module.setName(desc.name());
        module.setDesc(desc.desc());
        module.setRequires(desc.requires());
        Arrays.stream(clazz.getDeclaredFields()).forEachOrdered(f -> {
            if (f.isAnnotationPresent(ConfigurationProperty.class)) {
                ModuleConfigField field = loadModuleConfigField(f);
                module.getConfigFields().add(field);
            }
        });
        return module;
    }

    private ModuleConfigField loadModuleConfigField(Field f) {
        ModuleConfigField ret = new ModuleConfigField();
        ConfigurationProperty config = f.getDeclaredAnnotation(ConfigurationProperty.class);
        ret.setPath(config.path());
        ret.setDesc(config.desc());
        ret.setRequired(config.required());
        InputColumnProperty columnProp = f.getDeclaredAnnotation(InputColumnProperty.class);
        JavaType javaType = this.objectMapper.constructType(f.getGenericType());
        ret.setImpl(resolveTypedConfig(javaType, columnProp));
        return ret;
    }

    private TypedConfigurationField resolveTypedConfig(JavaType t, InputColumnProperty columnProp) {
        if (t.isArrayType() || t.isCollectionLikeType()) {
            return resolveTypedConfigCollection(t, columnProp);
        } else if (t.isPrimitive()) {
            return resolveTypedConfigPrimitive(t);
        } else if (t.isTypeOrSubTypeOf(String.class)) {
            return new StringTypedConfigurationField();
        } else if (t.isTypeOrSubTypeOf(Boolean.class)) {
            return new BooleanTypedConfigurationField();
        } else if (t.isTypeOrSubTypeOf(Number.class)) {
            return resolveTypedConfigNumber(t);
        } else if (t.isEnumImplType()) {
            return resolveTypedConfigEnum(t);
        } else if (t.isTypeOrSubTypeOf(JsonNode.class)) {
            return new JSONTypedConfigurationField(t.isTypeOrSubTypeOf(ObjectNode.class), t.isTypeOrSubTypeOf(ArrayNode.class));
        } else if (t.isMapLikeType()) {
            return resolveTypedConfigMap(t, columnProp);
        } else if (t.isTypeOrSubTypeOf(Schema.class)) {
            return new SchemaTypedConfigurationField();
        } else if (t.isTypeOrSubTypeOf(InputColumn.class)) {
            return columnProp == null ? new InputColumnTypedConfigurationField() : new InputColumnTypedConfigurationField(columnProp);
        } else if (!t.isConcrete()) {
            throw new IllegalArgumentException("Abstract classes and interfaces cannot be used for configuration parameters");
        } else {
            return resolveTypedConfigPOJO(t, columnProp);
        }
    }

    private CollectionTypedConfigurationField resolveTypedConfigCollection(JavaType t, InputColumnProperty columnProp) {
        CollectionTypedConfigurationField ret = new CollectionTypedConfigurationField();
        ret.setContents(resolveTypedConfig(t.getContentType(), columnProp));
        return ret;
    }

    private static TypedConfigurationField resolveTypedConfigPrimitive(JavaType t) {
        Class<?> clz = t.getRawClass();
        if (clz.equals(boolean.class)) {
            return new BooleanTypedConfigurationField();
        } else if (clz.equals(char.class)) {
            return new CharacterTypedConfigurationField();
        } else if (clz.equals(byte.class)) {
            return new NumericTypedConfigurationField(false, Byte.MIN_VALUE, Byte.MAX_VALUE);
        } else if (clz.equals(short.class)) {
            return new NumericTypedConfigurationField(false, Short.MIN_VALUE, Short.MAX_VALUE);
        } else if (clz.equals(int.class)) {
            return new NumericTypedConfigurationField(false, Integer.MIN_VALUE, Integer.MAX_VALUE);
        } else if (clz.equals(long.class)) {
            return new NumericTypedConfigurationField(false, Long.MIN_VALUE, Long.MAX_VALUE);
        } else if (clz.equals(float.class)) {
            return new NumericTypedConfigurationField(true, Float.MIN_VALUE, Float.MAX_VALUE);
        } else if (clz.equals(double.class)) {
            return new NumericTypedConfigurationField(true, Double.MIN_VALUE, Double.MAX_VALUE);
        } else {
            throw new IllegalArgumentException("Primitive is of type void");
        }
    }

    private TypedConfigurationField resolveTypedConfigNumber(JavaType t) {
        // TODO We cannot exactly process all possible implementations of Number, so default to floating/double and just
        // error out at runtime if incorrect
        return new NumericTypedConfigurationField(true, Double.MIN_VALUE, Double.MAX_VALUE);
    }

    private TypedConfigurationField resolveTypedConfigEnum(JavaType t) {
        return new EnumerationTypedConfigurationField(
                Arrays.stream(((Class<? extends Enum>) t.getRawClass()).getEnumConstants())
                        .map(f -> f.name())
                        .collect(Collectors.toList())
        );
    }

    private TypedConfigurationField resolveTypedConfigMap(JavaType t, InputColumnProperty columnProperty) {
        TypedConfigurationField key = resolveTypedConfig(t.getKeyType(), columnProperty);
        TypedConfigurationField value = resolveTypedConfig(t.getContentType(), columnProperty);
        return new MapTypedConfigurationField(key, value);
    }

    private TypedConfigurationField resolveTypedConfigPOJO(JavaType t, InputColumnProperty columnProperty) {
        Map<String, TypedConfigurationField> fields = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        for (Field f : t.getRawClass().getDeclaredFields()) {
            fields.put(f.getName(), resolveTypedConfig(om.constructType(f.getGenericType()), columnProperty));
        }
        return new ObjectTypedConfigurationField(fields);
    }
}

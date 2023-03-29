package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.annotations.ComponentDescription;
import org.ohnlp.backbone.api.annotations.ConfigurationProperty;
import org.ohnlp.backbone.configurator.structs.modules.ModuleConfigField;
import org.ohnlp.backbone.configurator.structs.modules.ModulePackageDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.types.*;
import org.ohnlp.backbone.configurator.util.ModuleClassLoader;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.type.filter.AssignableTypeFilter;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

public class ModuleRegistry {

    private static final ModuleRegistry INSTANCE = new ModuleRegistry();
    private static final Logger LOGGER = Logger.getGlobal();

    private final Set<File> files;

    private ModuleClassLoader classloader;
    private final ObjectMapper objectMapper;
    private Map<ModulePackageDeclaration, List<ModulePipelineComponentDeclaration>> componentDecsByPackage;
    private Map<String, ModulePipelineComponentDeclaration> componentDecsByClass;
    private Map<String, List<ModulePackageDeclaration>> packageDecsByClass;

    private ModuleRegistry() {
        this.files = new HashSet<>();
        this.classloader = new ModuleClassLoader(new URL[0], ClassLoader.getPlatformClassLoader());
        this.objectMapper = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(this.classloader));
    }

    public static void registerFiles(File... toAdd) {
        try {
            for (File f : toAdd) {
                if (INSTANCE.files.add(f)) {
                    INSTANCE.classloader.addURL(f.toURI().toURL());
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to add files to classloader via reflection", t);
        }
        INSTANCE.refreshRegistry();
    }

    public static ClassLoader getComponentClassLoader() {
        return INSTANCE.classloader;
    }

    public static Map<ModulePackageDeclaration, List<ModulePipelineComponentDeclaration>> getAllRegisteredComponents() {
        return INSTANCE.componentDecsByPackage;
    }

    public static ModulePipelineComponentDeclaration getComponentByClass(String clazz) {
        return INSTANCE.componentDecsByClass.get(clazz);
    }

    private void refreshRegistry() {
        try {
            scanForModules();
        } catch (IOException e) {
            throw new RuntimeException("Failed to scan supplied modules for component declarations", e);
        }
    }

    private void scanForModules() throws IOException {
        this.componentDecsByPackage = new HashMap<>();
        this.componentDecsByClass = new HashMap<>();
        this.packageDecsByClass = new HashMap<>();
        for (File f : this.files) {
            JarFile jar = new JarFile(f);
            ZipEntry entry = jar.getEntry("backbone_module.json");
            if (entry == null) {
                continue;
            }
            ModulePackageDeclaration packageDec = this.objectMapper.readValue(jar.getInputStream(entry), ModulePackageDeclaration.class);
            this.componentDecsByPackage.put(packageDec, new ArrayList<>());
            jar.close();
            ClassPathScanningCandidateComponentProvider provider
                    = new ClassPathScanningCandidateComponentProvider(true);
            provider.setResourceLoader(new DefaultResourceLoader(new URLClassLoader(this.classloader.getURLs())));
            provider.addIncludeFilter(new AssignableTypeFilter(BackbonePipelineComponent.class));
            // Scan packages for annotated entities
            if (packageDec.getPackages() != null) {
                new HashSet<>(packageDec.getPackages()).forEach(
                        basePackage -> {
                            Set<BeanDefinition> components = provider.findCandidateComponents(basePackage);
                            components.forEach(component -> {
                                try {
                                    Class<? extends BackbonePipelineComponent<?, ?>> clazz
                                            = (Class<? extends BackbonePipelineComponent<?, ?>>) Class.forName(component.getBeanClassName(), true, this.classloader);
                                    if (!Modifier.isAbstract(clazz.getModifiers())) {
                                        ModulePipelineComponentDeclaration componentDec = loadComponentDeclaration(clazz);
                                        this.componentDecsByPackage.get(packageDec).add(componentDec);
                                        this.packageDecsByClass.computeIfAbsent(clazz.getName(), k -> new ArrayList<>()).add(packageDec);
                                        this.componentDecsByClass.put(clazz.getName(), componentDec);
                                    }
                                } catch (ClassNotFoundException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                );
            }
        }
        // Scan for duplicate component declarations
        this.packageDecsByClass.forEach((clazz, decs) -> {
            if (decs.size() > 1) {
                LOGGER.warning("The component " + clazz + " is declared as part of multiple modules. " +
                        "This may lead to unexpected behaviour if versions clash! This component exists in: ");
                decs.forEach(s -> {
                    LOGGER.warning("- " + s.getName());
                });
            }
        });
    }

    /**
     * Scan the provided module's annotations and populate relevant fields in the module
     *
     * @param clazz The module class file
     */
    private ModulePipelineComponentDeclaration loadComponentDeclaration(Class<? extends BackbonePipelineComponent<?, ?>> clazz) {
        ModulePipelineComponentDeclaration module = new ModulePipelineComponentDeclaration(clazz);
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
        JavaType javaType = this.objectMapper.constructType(f.getGenericType());
        if (config.isInputColumn()) {
            ret.setImpl(new InputColumnTypedConfigurationField(javaType.isArrayType() || javaType.isCollectionLikeType()));
        } else {
            ret.setImpl(resolveTypedConfig(javaType));
        }
        return ret;
    }

    private TypedConfigurationField resolveTypedConfig(JavaType t) {
        if (t.isArrayType() || t.isCollectionLikeType()) {
            return resolveTypedConfigCollection(t);
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
            return resolveTypedConfigMap(t);
        } else if (t.isTypeOrSubTypeOf(Schema.class)) {
            return new SchemaTypedConfigurationField();
        } else if (!t.isConcrete()) {
            throw new IllegalArgumentException("Abstract classes and interfaces cannot be used for configuration parameters");
        } else {
            return resolveTypedConfigPOJO(t);
        }
    }

    private CollectionTypedConfigurationField resolveTypedConfigCollection(JavaType t) {
        CollectionTypedConfigurationField ret = new CollectionTypedConfigurationField();
        ret.setContents(resolveTypedConfig(t.getContentType()));
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

    private TypedConfigurationField resolveTypedConfigMap(JavaType t) {
        TypedConfigurationField key = resolveTypedConfig(t.getKeyType());
        TypedConfigurationField value = resolveTypedConfig(t.getContentType());
        return new MapTypedConfigurationField(key, value);
    }

    private TypedConfigurationField resolveTypedConfigPOJO(JavaType t) {
        Map<String, TypedConfigurationField> fields = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        for (Field f : t.getRawClass().getDeclaredFields()) {
            fields.put(f.getName(), resolveTypedConfig(om.constructType(f.getGenericType())));
        }
        return new ObjectTypedConfigurationField(fields);
    }
}

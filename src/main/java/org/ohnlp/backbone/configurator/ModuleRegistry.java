package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.ohnlp.backbone.configurator.structs.modules.*;
import org.ohnlp.backbone.configurator.util.ModuleClassLoader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ModuleRegistry {

    private static final ModuleRegistry INSTANCE = new ModuleRegistry();
    private static final Logger LOGGER = Logger.getGlobal();

    private final Set<File> javaModuleFiles;
    private final Set<File> pythonModuleFiles;

    private ModuleClassLoader classloader;
    private final ObjectMapper objectMapper;
    private List<ModulePackageDeclaration> modules;
    private Map<String, ModulePipelineComponentDeclaration> componentDecsByClass;
    private Map<String, List<ModulePackageDeclaration>> packageDecsByClass;

    private ModuleRegistry() {
        this.javaModuleFiles = new HashSet<>();
        this.pythonModuleFiles = new HashSet<>();
        this.classloader = new ModuleClassLoader(new URL[0], ClassLoader.getSystemClassLoader());
        this.objectMapper = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(this.classloader));
    }

    public static void registerJavaModules(File... toAdd) {
        if (toAdd == null) {
            return;
        }
        try {
            for (File f : toAdd) {
                if (INSTANCE.javaModuleFiles.add(f)) {
                    INSTANCE.classloader.addURL(f.toURI().toURL());
                }
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to add files to classloader via reflection", t);
        }
        INSTANCE.refreshRegistry();
    }
    public static void registerPythonModules(File... toAdd) {
        if (toAdd != null && toAdd.length > 0) {
            INSTANCE.pythonModuleFiles.addAll(Arrays.asList(toAdd));
            INSTANCE.refreshRegistry();
        }
    }

    public static ModuleClassLoader getComponentClassLoader() {
        return INSTANCE.classloader;
    }

    public static List<ModulePackageDeclaration> getAllRegisteredComponents() {
        return INSTANCE.modules;
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
        this.modules = new ArrayList<>();
        this.componentDecsByClass = new HashMap<>();
        this.packageDecsByClass = new HashMap<>();
        // Scan for Java Modules
        for (File f : this.javaModuleFiles) {
            JarFile jar = new JarFile(f);
            ZipEntry entry = jar.getEntry("backbone_module.json");
            if (entry == null) {
                continue;
            }
            ModulePackageDeclaration packageDec = this.objectMapper.readValue(jar.getInputStream(entry), ModulePackageDeclaration.class);
            this.modules.add(packageDec);
            jar.close();
        }
        // Scan for python modules
        for (File f : pythonModuleFiles) {
            ZipFile zip = new ZipFile(f);
            ZipEntry entry = zip.getEntry("backbone_module.json");
            if (entry == null) {
                continue;
            }
            ModulePackageDeclaration dec = this.objectMapper.readValue(zip.getInputStream(entry), ModulePackageDeclaration.class);
            this.modules.add(dec);
        }
        // Scan for duplicates
        this.modules.forEach(module -> {
            module.getComponents().forEach(component -> {
                if (component instanceof JavaModulePipelineComponentDeclaration) {
                    this.packageDecsByClass.computeIfAbsent(((JavaModulePipelineComponentDeclaration) component).getClazz().getName(), k -> new ArrayList<>()).add(module);
                    this.componentDecsByClass.put(((JavaModulePipelineComponentDeclaration) component).getClazz().getName(), component);
                } else if (component instanceof PythonModulePipelineComponentDeclaration) {
                    String key = ((PythonModulePipelineComponentDeclaration) component).getBundle_identifier() + ":" + ((PythonModulePipelineComponentDeclaration) component).getEntry_point() + ":" + ((PythonModulePipelineComponentDeclaration) component).getClass_name();
                    this.packageDecsByClass.computeIfAbsent(key, k -> new ArrayList<>()).add(module);
                    this.componentDecsByClass.put(key, component);
                }
            });
        });
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
}

package org.ohnlp.backbone.configurator.structs.modules;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohnlp.backbone.api.ComponentLang;
import org.ohnlp.backbone.configurator.structs.modules.serde.ModulePackageDeclarationDeserializer;
import org.ohnlp.backbone.configurator.structs.modules.serde.PythonModulePipelineComponentDeclarationDeserializer;

import java.util.List;

/**
 * A POJO Representing a Backbone Module parsed from backbone_module.json
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = ModulePackageDeclarationDeserializer.class)
public class ModulePackageDeclaration {
    private String name;
    private String desc;
    private String repo;
    private String version;
    private ComponentLang lang;
    private List<String> packages;
    private List<String> dependencies;

    private String moduleIdentifier;

    private List<ModulePipelineComponentDeclaration> components;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getRepo() {
        return repo;
    }

    public void setRepo(String repo) {
        this.repo = repo;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<String> getPackages() {
        return packages;
    }

    public void setPackages(List<String> packages) {
        this.packages = packages;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(List<String> dependencies) {
        this.dependencies = dependencies;
    }

    public List<ModulePipelineComponentDeclaration> getComponents() {
        return components;
    }

    public void setComponents(List<ModulePipelineComponentDeclaration> components) {
        this.components = components;
    }

    public ComponentLang getLang() {
        return lang;
    }

    public void setLang(ComponentLang lang) {
        this.lang = lang;
    }

    public String getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void setModuleIdentifier(String moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
    }
}

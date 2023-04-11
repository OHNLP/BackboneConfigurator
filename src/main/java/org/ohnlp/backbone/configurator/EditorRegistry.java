package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.IOException;

public class EditorRegistry {
    private static EditorRegistry INSTANCE = new EditorRegistry();

    private SimpleObjectProperty<ConfigManager.ConfigMeta> configMeta;
    private SimpleObjectProperty<EditablePipeline> pipeline;
    private SimpleObjectProperty<PipelineComponentDeclaration> currentEditedComponent;

    private SimpleBooleanProperty refreshGraph;

    public EditorRegistry() {
        this.configMeta = new SimpleObjectProperty<>();
        this.pipeline = new SimpleObjectProperty<>();
        this.currentEditedComponent = new SimpleObjectProperty<>();
        this.refreshGraph = new SimpleBooleanProperty(false);
    }

    public static SimpleObjectProperty<ConfigManager.ConfigMeta> getConfigMetadata() {
        return INSTANCE.configMeta;
    }

    public static void setCurrentConfig(ConfigManager.ConfigMeta currentConfig) throws IOException {
        INSTANCE.configMeta.setValue(currentConfig);
        BackboneConfiguration config = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(ModuleRegistry.getComponentClassLoader())).readValue(currentConfig.getFile(), BackboneConfiguration.class);
        INSTANCE.pipeline.setValue(EditablePipeline.fromConfig(config));
        INSTANCE.currentEditedComponent.setValue(null);
    }

    public static SimpleObjectProperty<EditablePipeline> getCurrentEditablePipeline() {
        return INSTANCE.pipeline;
    }

    public static SimpleObjectProperty<PipelineComponentDeclaration> getCurrentEditedComponent() {
        return INSTANCE.currentEditedComponent;
    }

    public static SimpleBooleanProperty refreshGraphProperty() {
        return INSTANCE.refreshGraph;
    }

    public static void reset() {
        INSTANCE.configMeta.setValue(null);
        INSTANCE.pipeline.setValue(null);
        INSTANCE.currentEditedComponent.setValue(null);
    }
}

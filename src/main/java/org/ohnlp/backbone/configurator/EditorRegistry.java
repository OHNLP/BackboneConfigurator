package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;

import java.io.IOException;

public class EditorRegistry {
    private static EditorRegistry INSTANCE = new EditorRegistry();

    private ConfigManager.ConfigMeta configMeta;
    private EditablePipeline pipeline;


    public static ConfigManager.ConfigMeta getConfigMetadata() {
        return INSTANCE.configMeta;
    }

    public static void setCurrentConfig(ConfigManager.ConfigMeta currentConfig) throws IOException {
        INSTANCE.configMeta = currentConfig;
        BackboneConfiguration config = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(ModuleRegistry.getComponentClassLoader())).readValue(currentConfig.getFile(), BackboneConfiguration.class);
        INSTANCE.pipeline = EditablePipeline.fromConfig(config);
    }

    public static EditablePipeline getCurrentEditablePipeline() {
        return INSTANCE.pipeline;
    }
}

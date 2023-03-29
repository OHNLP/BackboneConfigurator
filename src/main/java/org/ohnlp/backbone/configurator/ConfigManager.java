package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.ohnlp.backbone.api.config.BackboneConfiguration;

import java.io.File;
import java.io.IOException;

public class ConfigManager {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private ConfigManager() {

    }

    public BackboneConfiguration loadConfig(File f) throws IOException {
        return this.objectMapper.readValue(f, BackboneConfiguration.class);
    }
}

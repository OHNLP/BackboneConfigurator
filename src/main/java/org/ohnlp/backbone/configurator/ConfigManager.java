package org.ohnlp.backbone.configurator;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.ohnlp.backbone.api.config.BackboneConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConfigManager {

    private static final ConfigManager INSTANCE = new ConfigManager();

    private final ObjectMapper objectMapper;
    private final ObservableList<ConfigMeta> configs;
    private final ObjectMapper omWithoutClassResolution;

    private ConfigManager() {
        this.configs = FXCollections.observableArrayList(new ArrayList<>());
        this.objectMapper  = new ObjectMapper();
        this.omWithoutClassResolution = new ObjectMapper();
        // Mixin to replace class resolutions to string
        this.omWithoutClassResolution.addMixIn(Class.class, IgnoreTypeMixin.class);
        reloadConfigs();
    }

    private void reloadConfigs() {
        File[] files = new File("configs").listFiles();
        if (files == null) {
            files = new File[0];
        }
        this.configs.clear();
        this.configs.addAll(Arrays.stream(files).flatMap(f -> {
            ConfigMeta ret = new ConfigMeta();
            try {
                BackboneConfiguration config = this.omWithoutClassResolution.readValue(f, BackboneConfiguration.class);
                ret.setName(config.getId());
                ret.setDesc(config.getDescription());
                ret.setFile(f);
                ret.setLastModified(Files.getLastModifiedTime(f.toPath()).toInstant());
                ret.setBackingConfig(config);
                return Stream.of(ret);
            } catch (Throwable t) {
                Logger.getGlobal().warning("Invalid or malformed configuration found at " + f.getName() + ", skipping");
                return Stream.empty();
            }
        }).collect(Collectors.toList()));
        this.configs.sort((c1, c2) -> c2.getLastModified().compareTo(c1.getLastModified()));
    }

    public static void reload() {
        INSTANCE.reloadConfigs();
    }

    public static ObservableList<ConfigMeta> getConfigs() {
        return INSTANCE.configs;
    }

    public static void createConfig(String id, String desc, String file) throws IOException {
        BackboneConfiguration config = new BackboneConfiguration();
        config.setId(id);
        config.setDescription(desc);
        config.setPipeline(new ArrayList<>());
        // TODO file paths
        INSTANCE.objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File("configs", file), config);
        INSTANCE.reloadConfigs();
    }

    public static void deleteConfig(ConfigMeta config) {
        config.getFile().delete();
        INSTANCE.reloadConfigs();
    }

    public BackboneConfiguration loadConfig(File f) throws IOException {
        return this.objectMapper.readValue(f, BackboneConfiguration.class);
    }

    public static class ConfigMeta {
        private String name;
        private String desc;
        private File file;
        private Instant lastModified;

        private BackboneConfiguration backingConfig;


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

        public File getFile() {
            return file;
        }

        public void setFile(File file) {
            this.file = file;
        }

        public Instant getLastModified() {
            return lastModified;
        }

        public void setLastModified(Instant lastModified) {
            this.lastModified = lastModified;
        }

        public BackboneConfiguration getBackingConfig() {
            return backingConfig;
        }

        public void setBackingConfig(BackboneConfiguration backingConfig) {
            this.backingConfig = backingConfig;
        }
    }

    @JsonIgnoreType
    public static class IgnoreTypeMixin {}
}

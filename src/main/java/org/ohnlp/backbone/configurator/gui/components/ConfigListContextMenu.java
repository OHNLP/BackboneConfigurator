package org.ohnlp.backbone.configurator.gui.components;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import org.ohnlp.backbone.configurator.ConfigManager;

public class ConfigListContextMenu extends ContextMenu {
    private final ConfigManager.ConfigMeta sourceConfig;

    public ConfigListContextMenu(ConfigManager.ConfigMeta sourceConfig) {
        super();
        this.sourceConfig = sourceConfig;
        MenuItem open = new MenuItem("Open");
        MenuItem edit = new MenuItem("Edit Metadata");
        MenuItem delete = new MenuItem("Delete");
        getItems().addAll(
                open,
                edit,
                new SeparatorMenuItem(),
                delete
        );
    }
}

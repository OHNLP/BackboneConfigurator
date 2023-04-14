package org.ohnlp.backbone.configurator.gui.components;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import org.ohnlp.backbone.configurator.ConfigManager;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.Views;

import java.io.IOException;

public class ConfigListContextMenu extends ContextMenu {

    public ConfigListContextMenu(ConfigManager.ConfigMeta sourceConfig) {
        super();
        MenuItem open = new MenuItem("Open");
        MenuItem delete = new MenuItem("Delete");
        getItems().addAll(
                open,
                delete
        );
        open.setOnAction((e) -> {
            try {
                EditorRegistry.setCurrentConfig(sourceConfig);
                Views.openView(Views.ViewType.PIPELINE_EDITOR, getScene().getWindow(), true);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        });
        delete.setOnAction((e) -> {
            try {
                Views.displayConfirmationDialog("Delete Config?", "Are you sure you want to delete " + sourceConfig.getName() + "?", () -> ConfigManager.deleteConfig(sourceConfig), () -> {});
            } catch (Views.DialogCancelledException ignored) {}
        });
    }
}

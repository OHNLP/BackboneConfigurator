package org.ohnlp.backbone.configurator.gui.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import org.apache.beam.repackaged.core.org.apache.commons.lang3.SystemUtils;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.configurator.ConfigManager;
import org.ohnlp.backbone.configurator.gui.components.TitleBar;

import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Logger;

public class NewPipelineDialogController {
    @FXML
    public TextField name;
    @FXML
    public TextField desc;
    @FXML
    public TextField file;
    @FXML
    public TitleBar titlebar;

    @FXML
    public void initialize() {
        // Determine whether title bar should be visible (disabled for Mac)
        if (SystemUtils.IS_OS_MAC_OSX) {
            titlebar.setVisible(false);
        }
    }

    @FXML
    public void createPipeline(ActionEvent event) {
        BackboneConfiguration config = new BackboneConfiguration();
        config.setId(name.getText());
        config.setDescription(desc.getText());
        String nm = file.getText();
        if (!nm.toLowerCase(Locale.ROOT).endsWith(".json")) {
            nm += ".json";
        }
        File out = new File("configs", nm);
        if (!out.exists()) {
            try {
                ConfigManager.createConfig(name.getText(), desc.getText(), nm);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            Logger.getGlobal().warning("Requested creation of " + nm + " which already exists. Skipping"); // TODO update prompt fields with invalid value warning instead
        }
        ((Node)event.getSource()).getScene().getWindow().hide();
    }

    @FXML
    public void close(ActionEvent event) {
        ((Node)event.getSource()).getScene().getWindow().hide();
    }
}

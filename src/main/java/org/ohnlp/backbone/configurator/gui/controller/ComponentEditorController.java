package org.ohnlp.backbone.configurator.gui.controller;

import javafx.fxml.FXML;
import javafx.scene.layout.Pane;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.springframework.util.Assert;

public class ComponentEditorController {
    @FXML
    public Pane mainPane;

    @FXML
    public void initialize() {
        Assert.notNull(EditorRegistry.getCurrentEditedComponent(), "Component Editor Dialog Initialized with Null Component");
    }
}

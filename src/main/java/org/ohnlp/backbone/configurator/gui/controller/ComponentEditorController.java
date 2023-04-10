package org.ohnlp.backbone.configurator.gui.controller;

import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;
import org.springframework.util.Assert;

public class ComponentEditorController {
    @FXML
    public VBox configList;
    @FXML
    public AnchorPane container;

    @FXML
    public void initialize() {
        Assert.notNull(EditorRegistry.getCurrentEditedComponent(), "Component Editor Dialog Initialized with Null Component");
        SimpleObjectProperty<PipelineComponentDeclaration> edited = EditorRegistry.getCurrentEditedComponent();
        configList.prefWidthProperty().bind(container.widthProperty());
        TitledPane stepIDPrompt = new TitledPane("Step ID", new TextField(edited.get().getComponentID()));
        configList.getChildren().add(stepIDPrompt);
        // TODO generate inputs prompt
        edited.get().getConfig().forEach(f -> {
            TitledPane p = new TitledPane();
            p.setText(f.getDesc());
            p.setContent(f.getImpl().render());
            configList.getChildren().add(p);
        });
    }
}

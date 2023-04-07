package org.ohnlp.backbone.configurator.gui.controller;

import com.fxgraph.graph.Graph;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import org.abego.treelayout.Configuration;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.gui.components.graphs.LabelPositionAbegoTreeLayout;
import org.ohnlp.backbone.configurator.gui.utils.DAGUtils;

public class PipelineEditorController {
    @FXML
    public Pane pipelineDisplay;
    @FXML
    public AnchorPane container;
    @FXML
    public HBox toolbar;
    @FXML
    public Button removeStepButton;
    @FXML
    public Button editStepButton;

    @FXML
    public void initialize() {
        // Generate pipeline graph
        Graph g = DAGUtils.generateGraphForPipeline(EditorRegistry.getCurrentEditablePipeline().getValue());
        g.layout(new LabelPositionAbegoTreeLayout(150, 500, Configuration.Location.Top));
        this.pipelineDisplay.getChildren().add(new BorderPane(g.getCanvas()));
        // Toolbar Display Options
        this.toolbar.prefWidthProperty().bind(this.container.widthProperty());
        DropShadow shadow = new DropShadow();
        this.toolbar.effectProperty().set(shadow);
        // Bind remove/edit step to whether a component is selected for editing
        BooleanBinding stepNotSelected = EditorRegistry.getCurrentEditedComponent().isNull();
        removeStepButton.disableProperty().bind(stepNotSelected);
        editStepButton.disableProperty().bind(stepNotSelected);
    }
}

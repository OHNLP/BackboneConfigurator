package org.ohnlp.backbone.configurator.gui.controller;

import com.fxgraph.graph.Graph;
import javafx.fxml.FXML;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import org.abego.treelayout.Configuration;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.gui.components.graphs.LabelPositionAbegoTreeLayout;
import org.ohnlp.backbone.configurator.gui.utils.DAGUtils;

public class PipelineEditorController {
    @FXML
    public Pane pipelineDisplay;

    @FXML
    public void initialize() {
        Graph g = DAGUtils.generateGraphForPipeline(EditorRegistry.getCurrentEditablePipeline());
        g.layout(new LabelPositionAbegoTreeLayout(150, 500, Configuration.Location.Top));
        this.pipelineDisplay.getChildren().add(new BorderPane(g.getCanvas()));
    }
}

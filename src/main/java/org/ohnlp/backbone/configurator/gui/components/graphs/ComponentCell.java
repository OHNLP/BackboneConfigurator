package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.cells.CellGestures;
import com.fxgraph.cells.RectangleCell;
import com.fxgraph.graph.Graph;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.ExtractComponent;
import org.ohnlp.backbone.api.components.LoadComponent;

import java.util.List;

public class ComponentCell extends RectangleCell {
    private final String name;
    private final BackbonePipelineComponent instance;
    private final String id;

    public ComponentCell(String id, String name, String desc, List<String> inputs, List<String> outputs, BackbonePipelineComponent instance) {
        this.id = id;
        this.name = name;
        this.instance = instance;
    }

    @Override
    public Region getGraphic(Graph graph) {
        final Rectangle view = new Rectangle(250, 50);

        Color color;
        if (instance instanceof ExtractComponent) {
            color = Color.FORESTGREEN;
        } else if (instance instanceof LoadComponent) {
            color = Color.RED;
        } else {
            color = Color.DODGERBLUE;
        }
        view.setStroke(color);
        view.setFill(color);
        StackPane stack = new StackPane();
        Text text = new Text(this.id + "\r\n" + this.name);
        text.setTextAlignment(TextAlignment.CENTER);
        stack.getChildren().addAll(view, text);

        final Pane pane = new Pane(stack);
        pane.setPrefSize(250, 50);
        view.widthProperty().bind(pane.prefWidthProperty());
        view.heightProperty().bind(pane.prefHeightProperty());
        CellGestures.makeResizable(pane);

        return pane;
    }
}

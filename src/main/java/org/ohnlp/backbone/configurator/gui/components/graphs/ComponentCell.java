package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.cells.RectangleCell;
import com.fxgraph.graph.Graph;
import javafx.geometry.Orientation;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.ExtractComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.api.components.LoadComponent;

import java.util.List;

public class ComponentCell extends RectangleCell {
    private final String name;
    final BackbonePipelineComponent instance;
    private final String id;
    private final List<String> inputs;
    private final List<String> outputs;
    private int srcOutputIndex = -1;
    private int inputIdx = -1;

    public ComponentCell(String id, String name, String desc, List<String> inputs, List<String> outputs, BackbonePipelineComponent instance) {
        this.id = id;
        this.name = name;
        this.instance = instance;
        this.inputs = inputs;
        this.outputs = outputs;
    }

    public boolean hasInput() {
        return instance instanceof HasInputs;
    }

    public boolean hasOutput() {
        return instance instanceof HasOutputs;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    @Override
    public Region getGraphic(Graph graph) {
        BorderPane node = new BorderPane();
        FlowPane inputPane  = new FlowPane(Orientation.HORIZONTAL);
        inputPane.setHgap(-1);
        inputPane.setPrefHeight(20);
        FlowPane outputPane = new FlowPane(Orientation.HORIZONTAL);
        outputPane.setHgap(-1);
        outputPane.setPrefHeight(20);
        Color color;
        if (instance instanceof ExtractComponent) {
            color = Color.FORESTGREEN;
        } else if (instance instanceof LoadComponent) {
            color = Color.RED;
        } else {
            color = Color.DODGERBLUE;
        }
        if (!this.inputs.isEmpty()) {
            int cnt = this.inputs.size();
            int width = (int) Math.round(Math.floor(250f/cnt));
            for (int i = 0; i < this.inputs.size(); i++) {
                String label = this.inputs.get(i);
                Rectangle bckgnd = new Rectangle(i == this.inputs.size() - 1 ? (250 - width * i) : width, 20);
                bckgnd.setFill(Color.LIGHTYELLOW);
                bckgnd.setStroke(Color.BLACK);
                Text labelText = new Text(label);
                labelText.setTextAlignment(TextAlignment.CENTER);
                StackPane labelPane = new StackPane(bckgnd, labelText);
                inputPane.getChildren().add(labelPane);
            }
        } else {
            Rectangle bckgnd = new Rectangle(250, 20);
            bckgnd.setFill(Color.DARKGRAY);
            bckgnd.setStroke(Color.BLACK);
            Text labelText = new Text("No Inputs");
            labelText.setTextAlignment(TextAlignment.CENTER);
            StackPane labelPane = new StackPane(bckgnd, labelText);
            inputPane.getChildren().add(labelPane);
        }
        final Rectangle componentBackground = new Rectangle(250, 40);
        componentBackground.setStroke(Color.BLACK);
        componentBackground.setFill(color);
        Text componentLabel = new Text(this.id + "\r\n" + this.name);
        componentLabel.setTextAlignment(TextAlignment.CENTER);
        StackPane layeredComponentPane = new StackPane(componentBackground, componentLabel);
        Pane componentPane = new Pane(layeredComponentPane);
        componentPane.setPrefSize(250, 40);

        if (!this.outputs.isEmpty()) {
            int cnt = this.outputs.size();
            int width = (int) Math.round(Math.floor(250f/cnt));
            for (int i = 0; i < this.outputs.size(); i++) {
                String label = this.outputs.get(i);
                Rectangle bckgnd = new Rectangle(i == this.outputs.size() - 1 ? (250 - width * i) : width, 20);
                bckgnd.setFill(Color.LIGHTYELLOW);
                bckgnd.setStroke(Color.BLACK);
                Text labelText = new Text(label);
                labelText.setTextAlignment(TextAlignment.CENTER);
                StackPane labelPane = new StackPane(bckgnd, labelText);
                outputPane.getChildren().add(labelPane);
            }
        } else {
            Rectangle bckgnd = new Rectangle(250, 20);
            bckgnd.setFill(Color.DARKGRAY);
            bckgnd.setStroke(Color.BLACK);
            Text labelText = new Text("No Outputs");
            labelText.setTextAlignment(TextAlignment.CENTER);
            StackPane labelPane = new StackPane(bckgnd, labelText);
            outputPane.getChildren().add(labelPane);
        }

        // Now build the view

        node.setTop(inputPane);
        node.setCenter(componentPane);
        node.setBottom(outputPane);
        node.setPrefWidth(250);

//        CellGestures.makeResizable(node);

        return new Pane(node);
    }


    public void setSrcOutputIndex(int idx) {
        this.srcOutputIndex = Math.max(srcOutputIndex, idx);
    }

    public void setInputIdx(int idx) {
        this.inputIdx = Math.max(inputIdx, idx);
    }

    public int getSrcOutputIndex() {
        return srcOutputIndex;
    }

    public int getInputIdx() {
        return inputIdx;
    }
}

package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.cells.RectangleCell;
import com.fxgraph.graph.Graph;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.ExtractComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.api.components.LoadComponent;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.gui.controller.PipelineEditorController;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.IOException;
import java.util.List;

public class ComponentCell extends RectangleCell {
    private final String name;
    final BackbonePipelineComponent instance;
    private final String id;
    private final List<String> inputs;
    private final List<String> outputs;
    private int srcOutputIndex = -1;
    private int inputIdx = -1;
    private PipelineComponentDeclaration pipelineDec;

    public ComponentCell(String id, String name, String desc, List<String> inputs, List<String> outputs, BackbonePipelineComponent instance, PipelineComponentDeclaration pipelineDec) {
        this.id = id;
        this.name = name;
        this.instance = instance;
        this.inputs = inputs;
        this.outputs = outputs;
        this.pipelineDec = pipelineDec;
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
        ComponentCellPane node = new ComponentCellPane();
        node.getStyleClass().add("graph-cell");
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
        // Calculate main components first so we know what to use for width
        Text componentLabel = new Text(this.id + "\r\n" + this.name);
        componentLabel.setTextAlignment(TextAlignment.CENTER);
//        double mainTextWidth = componentLabel.getLayoutBounds().getWidth() + 50;
        double mainTextWidth = 350; // TODO dynamically calculate based on max of children text nodes/label sums
        final Rectangle componentBackground = new Rectangle(mainTextWidth, 40);
        componentBackground.setStroke(Color.BLACK);
        componentBackground.setFill(color);
        StackPane layeredComponentPane = new StackPane(componentBackground, componentLabel);
        Pane componentPane = new Pane(layeredComponentPane);
        componentPane.setPrefSize(mainTextWidth, 40);
        if (!this.inputs.isEmpty()) {
            int cnt = this.inputs.size();
            for (int i = 0; i < this.inputs.size(); i++) {
                String label = this.inputs.get(i);
                Rectangle bckgnd = new Rectangle(i == this.inputs.size() - 1 ? (mainTextWidth - Math.floor(mainTextWidth/cnt) * i) : Math.floor(mainTextWidth/cnt), 20);
                bckgnd.setFill(Color.LIGHTYELLOW);
                bckgnd.setStroke(Color.BLACK);
                Text labelText = new Text(label);
                labelText.setTextAlignment(TextAlignment.CENTER);
                StackPane labelPane = new StackPane(bckgnd, labelText);
                inputPane.getChildren().add(labelPane);
            }
        } else {
            Rectangle bckgnd = new Rectangle(mainTextWidth, 20);
            bckgnd.setFill(Color.DARKGRAY);
            bckgnd.setStroke(Color.BLACK);
            Text labelText = new Text("No Inputs");
            labelText.setTextAlignment(TextAlignment.CENTER);
            StackPane labelPane = new StackPane(bckgnd, labelText);
            inputPane.getChildren().add(labelPane);
        }

        if (!this.outputs.isEmpty()) {
            int cnt = this.outputs.size();
            for (int i = 0; i < this.outputs.size(); i++) {
                String label = this.outputs.get(i);
                Rectangle bckgnd = new Rectangle(i == this.outputs.size() - 1 ? (mainTextWidth - Math.floor(mainTextWidth/cnt) * i) : Math.floor(mainTextWidth/cnt), 20);
                bckgnd.setFill(Color.LIGHTYELLOW);
                bckgnd.setStroke(Color.BLACK);
                Text labelText = new Text(label);
                labelText.setTextAlignment(TextAlignment.CENTER);
                StackPane labelPane = new StackPane(bckgnd, labelText);
                outputPane.getChildren().add(labelPane);
            }
        } else {
            Rectangle bckgnd = new Rectangle(mainTextWidth, 20);
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
        node.setPrefWidth(mainTextWidth);

        // Add a click listener to trigger selected
        node.onMousePressedProperty().setValue(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                EditorRegistry.getCurrentEditedComponent().setValue(this.pipelineDec);
                if (e.getClickCount() > 1) {
                    try {
                        // Since component editor can lead to schema re-resolution, display here first
                        Dialog<Boolean> alert = new Dialog();
                        alert.initStyle(StageStyle.UNDECORATED);
                        alert.setTitle("Resolving Input/Output Schemas");
                        alert.setHeaderText("Attempting to Resolve Input/Output Schemas");
                        alert.setContentText("Please Wait...");
                        alert.getDialogPane().getStyleClass().add("window");
                        alert.getDialogPane().getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
                        alert.show();
                        Platform.runLater(() -> {
                            FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-editor-view.fxml"));
                            Stage stage = new Stage();
                            stage.setTitle("Edit Pipeline Step");
                            Scene s = null;
                            try {
                                s = new Scene(loader.load());
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                            s.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
                            stage.setScene(s);
                            stage.initStyle(StageStyle.UNDECORATED);
                            stage.show();
                            alert.setResult(true);
                            alert.close();
                        });
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                }
            }
        });
        BooleanBinding selected = EditorRegistry.getCurrentEditedComponent().isEqualTo(this.pipelineDec);
        node.styleProperty().bind(Bindings.createStringBinding(() -> {
            if (selected.get()) {
                return "-fx-border-color: #3D84B9FF; -fx-border-width: 5px; ";
            } else {
                return "";
            }
        }, selected));


//        CellGestures.makeResizable(node);

        return node;
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

    public static class ComponentCellPane extends BorderPane {


        public ComponentCellPane() {
            super();
            this.getStyleClass().add("component_cell_pane");
        }
    }
}

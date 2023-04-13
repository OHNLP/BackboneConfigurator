package org.ohnlp.backbone.configurator.gui.controller;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.PannableCanvas;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TabPane;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.abego.treelayout.Configuration;
import org.ohnlp.backbone.configurator.ConfigManager;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.gui.ConfiguratorGUI;
import org.ohnlp.backbone.configurator.gui.components.graphs.LabelPositionAbegoTreeLayout;
import org.ohnlp.backbone.configurator.gui.utils.DAGUtils;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.IOException;

public class PipelineEditorController {
    @FXML
    public AnchorPane pipelineDisplay;
    @FXML
    public AnchorPane container;
    @FXML
    public HBox toolbar;
    @FXML
    public Button savePipelineButton;
    @FXML
    public Button removeStepButton;
    @FXML
    public Button editStepButton;
    @FXML
    public StackPane compass;
    private BorderPane renderedGraph;

    @FXML
    public void initialize() {
        // Generate pipeline graph
        Graph g = DAGUtils.generateGraphForPipeline(EditorRegistry.getCurrentEditablePipeline().getValue());
        g.layout(new LabelPositionAbegoTreeLayout(150, 500, Configuration.Location.Top));
        PannableCanvas canvas = g.getCanvas();
        canvas.prefWidthProperty().bind(container.widthProperty());
        this.renderedGraph = new BorderPane(canvas);
        this.pipelineDisplay.getChildren().add(renderedGraph);
        this.renderedGraph.prefWidthProperty().bind(pipelineDisplay.widthProperty());
        this.renderedGraph.prefHeightProperty().bind(pipelineDisplay.heightProperty());
        this.pipelineDisplay.viewOrderProperty().set(Double.MAX_VALUE); // Move to back
        // Toolbar Display Options
        this.toolbar.prefWidthProperty().bind(this.container.widthProperty());
        DropShadow shadow = new DropShadow();
        this.toolbar.effectProperty().set(shadow);
        // Bind remove/edit step to whether a component is selected for editing
        BooleanBinding stepNotSelected = EditorRegistry.getCurrentEditedComponent().isNull();
        removeStepButton.disableProperty().bind(stepNotSelected);
        editStepButton.disableProperty().bind(stepNotSelected);
        // Bind save to whether or not pipeline is currently dirty
        savePipelineButton.disableProperty().bind(Bindings.createBooleanBinding(() -> !EditorRegistry.getCurrentEditablePipeline().get().dirtyProperty().getValue(), EditorRegistry.getCurrentEditablePipeline(), EditorRegistry.getCurrentEditablePipeline().get().dirtyProperty()));
        // Add listener to refresh property to know when to refresh graph
        EditorRegistry.refreshGraphProperty().addListener((e, o, n) -> {
            if (n) {
                Graph newGraph = DAGUtils.generateGraphForPipeline(EditorRegistry.getCurrentEditablePipeline().getValue());
                newGraph.layout(new LabelPositionAbegoTreeLayout(150, 500, Configuration.Location.Top));
                this.renderedGraph.setCenter(newGraph.getCanvas());
                PannableCanvas ncanvas = newGraph.getCanvas();
                ncanvas.prefWidthProperty().bind(container.widthProperty());
                EditorRegistry.refreshGraphProperty().set(false);
            }
        });
        // Generate arrow buttons for pipeline scroll
//        Button right = new Button();
//        right.getStyleClass().add("compass-button");
//        StackPane.setAlignment(right, Pos.CENTER_RIGHT);
//        Button down = new Button();
//        down.setRotate(90);
//        down.getStyleClass().add("compass-button");
//        StackPane.setAlignment(down, Pos.BOTTOM_CENTER);
//        Button left = new Button();
//        left.setRotate(180);
//        left.getStyleClass().add("compass-button");
//        StackPane.setAlignment(left, Pos.CENTER_LEFT);
//        Button up = new Button();
//        up.setRotate(270);
//        up.getStyleClass().add("compass-button");
//        StackPane.setAlignment(up, Pos.TOP_CENTER);
//        Button center = new Button("o");
//        StackPane.setMargin(up, new Insets(10, 0, 0, 0));
//        StackPane.setMargin(right, new Insets(0, 10, 0, 0));
//        StackPane.setMargin(down, new Insets(0, 0, 10, 0));
//        StackPane.setMargin(left, new Insets(0, 0, 0, 10));
//        compass.getChildren().addAll(center, left, right, up, down);
//        compass.setPrefSize(64, 80);

    }

    @FXML
    public void onEdit(ActionEvent event) {
        PipelineComponentDeclaration active = EditorRegistry.getCurrentEditedComponent().get();
        if (active == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-editor-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Edit Pipeline Step");
            Scene s = new Scene(loader.load());
            s.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
            stage.setScene(s);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onClose(ActionEvent e) {
        // TODO track for dirty changes
        try {
            Stage stage = new Stage();
            FXMLLoader fxmlLoader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/welcome-view.fxml"));
            Scene scene = new Scene(fxmlLoader.load());
            scene.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("OHNLP Toolkit Pipeline Configuration Editor");
            stage.show();
            ((Node)e.getSource()).getScene().getWindow().hide();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    @FXML
    public void savePipeline(ActionEvent actionEvent) {
        try {
            EditorRegistry.getCurrentEditablePipeline().get().save(EditorRegistry.getConfigMetadata().get().getFile());
        } catch (Throwable t) {
            throw new RuntimeException(t); // TODO do not fail silently here
        }
    }

    @FXML
    public void onReload(ActionEvent actionEvent) throws IOException {
        EditorRegistry.setCurrentConfig(EditorRegistry.getConfigMetadata().get());
        EditorRegistry.refreshGraphProperty().set(true);
    }

    @FXML
    public void onAddComponent(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-browser-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Add Pipeline Component");
            Scene s = new Scene(loader.load());
            s.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
            s.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/component-browser-view.css").toExternalForm());
            stage.setScene(s);
            stage.show();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onClickRemoveStep(ActionEvent e) {
        if (EditorRegistry.getCurrentEditedComponent().isNotNull().get()) {
            EditorRegistry.getCurrentEditablePipeline().get().removeComponent(EditorRegistry.getCurrentEditedComponent().get());
            EditorRegistry.refreshGraphProperty().set(true);
        }
    }
}

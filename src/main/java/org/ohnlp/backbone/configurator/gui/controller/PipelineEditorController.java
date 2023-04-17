package org.ohnlp.backbone.configurator.gui.controller;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.PannableCanvas;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import org.abego.treelayout.Configuration;
import org.ohnlp.backbone.configurator.ConfigManager;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.Views;
import org.ohnlp.backbone.configurator.gui.components.TitleBar;
import org.ohnlp.backbone.configurator.gui.components.graphs.LabelPositionAbegoTreeLayout;
import org.ohnlp.backbone.configurator.gui.utils.DAGUtils;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.IOException;

public class PipelineEditorController {
    @FXML
    public Pane pipelineDisplay;
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
    @FXML
    public TitleBar titlebar;
    @FXML
    public VBox window;
    private BorderPane renderedGraph;

    private static Stage currEditorPane;

    @FXML
    public void initialize() {
        Platform.runLater(() -> {
            this.container.prefWidthProperty().bind(this.container.getScene().getWindow().widthProperty());
            this.window.prefHeightProperty().bind(this.container.getScene().getWindow().heightProperty());
        });

        this.titlebar.prefWidthProperty().bind(container.widthProperty());
        // Generate pipeline graph
        Graph g = DAGUtils.generateGraphForPipeline(EditorRegistry.getCurrentEditablePipeline().getValue());
        g.layout(new LabelPositionAbegoTreeLayout(150, 500, Configuration.Location.Top));
        PannableCanvas canvas = g.getCanvas();
        canvas.prefWidthProperty().bind(container.widthProperty());
        this.renderedGraph = new BorderPane(canvas);
        this.pipelineDisplay.getChildren().add(renderedGraph);
        this.renderedGraph.minHeightProperty().set(200);
        this.pipelineDisplay.viewOrderProperty().set(Double.MAX_VALUE); // Move to back
        // Toolbar Display Options
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
    }

    @FXML
    public void onEdit(ActionEvent event) {
        PipelineComponentDeclaration active = EditorRegistry.getCurrentEditedComponent().get();
        if (active == null) {
            return;
        }
        try {
            Views.openView(Views.ViewType.COMPONENT_EDITOR);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onClose(ActionEvent e) throws IOException {
        if (EditorRegistry.getCurrentEditablePipeline().isNotNull().get() && EditorRegistry.getCurrentEditablePipeline().get().dirtyProperty().getValue()) {
            try {
                Views.displayUncommitedSaveDialog("module configuration", () -> savePipeline(e), () -> {
                    try {
                        onReload(e);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
            } catch (Views.DialogCancelledException t) {
                return;
            }
        }
        try {
            Views.openView(Views.ViewType.CONFIG_LIST, ((Node)e.getSource()).getScene().getWindow(), true);
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    @FXML
    public void savePipeline(ActionEvent actionEvent) {
        try {
            EditorRegistry.getCurrentEditablePipeline().get().save(EditorRegistry.getConfigMetadata().get().getFile());
            ConfigManager.reload();
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
            Views.openView(Views.ViewType.COMPONENT_BROWSER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void onClickRemoveStep(ActionEvent e) {
        if (EditorRegistry.getCurrentEditedComponent().isNotNull().get()) {
            PipelineComponentDeclaration currComponent = EditorRegistry.getCurrentEditedComponent().get();
            try {
                Views.displayConfirmationDialog("Delete Component?",
                        "Are you sure you want to delete " + currComponent.getComponentID() + "?",
                        () -> {
                            EditorRegistry.getCurrentEditablePipeline().get().removeComponent(currComponent);
                            EditorRegistry.refreshGraphProperty().set(true);
                        },
                        () -> {}
                );
            } catch (Views.DialogCancelledException ignored) {
            }
        }
    }
}

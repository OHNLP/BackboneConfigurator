package org.ohnlp.backbone.configurator.gui.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.structs.modules.ModuleConfigField;
import org.ohnlp.backbone.configurator.structs.modules.ModulePackageDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class ComponentBrowserController {
    @FXML
    public ListView<ModulePackageDeclaration> moduleList;

    @FXML
    public ListView<ModulePipelineComponentDeclaration> componentList;
    @FXML
    public Pane container;

    @FXML
    public SplitPane contentPane;
    @FXML
    public AnchorPane window;
    @FXML
    public HBox buttonBar;
    @FXML
    public VBox componentPane;

    @FXML
    public Button okButton;

    @FXML
    public void initialize() {
        // Populate Module List
        moduleList.getItems().clear();
        moduleList.getItems().addAll(FXCollections.observableArrayList(ModuleRegistry.getAllRegisteredComponents().keySet()).sorted(Comparator.comparing(ModulePackageDeclaration::getName)));
        moduleList.setCellFactory(new ModulePackageCellFactory());
        // Bind to populate component list
        moduleList.getSelectionModel().selectedItemProperty().addListener((e, o, n) -> {
            componentList.getItems().clear();
            componentList.getSelectionModel().clearSelection();
            componentList.getItems().addAll(FXCollections.observableArrayList(ModuleRegistry.getAllRegisteredComponents().getOrDefault(n, Collections.emptyList())).sorted(Comparator.comparing(ModulePipelineComponentDeclaration::getName)));
        });
        componentList.setCellFactory(new ComponentCellFactory());
        // Bind ok button to require selection
        okButton.disableProperty().bind(componentList.getSelectionModel().selectedItemProperty().isNull());
        // Bindings for layout
        componentList.prefWidthProperty().bind(componentPane.prefWidthProperty());
        container.prefWidthProperty().bind(window.widthProperty());
        container.prefHeightProperty().bind(window.heightProperty());
        contentPane.prefWidthProperty().bind(container.widthProperty());
        contentPane.prefHeightProperty().bind(container.heightProperty());
        componentPane.prefWidthProperty().bind(container.widthProperty().subtract(moduleList.widthProperty()));
        componentPane.prefHeightProperty().bind(container.heightProperty());
    }

    @FXML
    public void onOK(MouseEvent e) {
        ModulePipelineComponentDeclaration selected = componentList.getSelectionModel().getSelectedItem();
        PipelineComponentDeclaration pcd = new PipelineComponentDeclaration();
        pcd.setComponentDef(selected);
        pcd.setInputs(new HashMap<>());
        pcd.setConfig(new ArrayList<>());
        selected.getConfigFields().forEach(f -> {
            ModuleConfigField cln = f.clone();
            cln.getImpl().reset(); // Because the source we are cloning from may have observable set already due to different window
            pcd.getConfig().add(cln);
        });
        EditorRegistry.getCurrentEditedComponent().set(pcd);
        EditorRegistry.inCreateNewComponentState().set(true);
        ((Node) e.getSource()).getScene().getWindow().hide();
        try {
            FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-editor-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Edit Pipeline Step");
            Scene s = new Scene(loader.load());
            s.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
            stage.setScene(s);
            stage.show();
        } catch (IOException t) {
            throw new RuntimeException(t);
        }
    }

    @FXML
    public void onCancel(MouseEvent e) {
        ((Node) e.getSource()).getScene().getWindow().hide();
    }

    public static class ModulePackageCellFactory implements Callback<ListView<ModulePackageDeclaration>, ListCell<ModulePackageDeclaration>> {

        @Override
        public ListCell<ModulePackageDeclaration> call(ListView<ModulePackageDeclaration> param) {
            return new ListCell<>() {
                @Override
                public void updateItem(ModulePackageDeclaration config, boolean empty) {
                    super.updateItem(config, empty);
                    setText(null);
                    if (empty || config == null) {
                        setGraphic(null);
                    } else {
                        HBox ret = new HBox();
                        VBox cell = new VBox();
                        cell.getChildren().add(new Text(config.getName()));
                        cell.getChildren().add(new Text(config.getRepo()));
                        cell.getChildren().add(new Text(config.getVersion()));
                        cell.getChildren().forEach(t -> t.getStyleClass().add("text-display"));
                        ret.getChildren().add(cell);
                        HBox.setHgrow(cell, Priority.ALWAYS);
                        Text arrow = new Text(">");
                        arrow.getStyleClass().add("text-display");
                        VBox right = new VBox();
                        Pane upRight = new Pane();
                        Pane downRight = new Pane();
                        right.getChildren().addAll(upRight, arrow, downRight);
                        VBox.setVgrow(upRight, Priority.ALWAYS);
                        VBox.setVgrow(downRight, Priority.ALWAYS);
                        ret.getChildren().add(right);
                        ret.getStyleClass().add("module-entry");
                        setGraphic(ret);
                    }
                }
            };
        }
    }

    public static class ComponentCellFactory implements Callback<ListView<ModulePipelineComponentDeclaration>, ListCell<ModulePipelineComponentDeclaration>> {

        @Override
        public ListCell<ModulePipelineComponentDeclaration> call(ListView<ModulePipelineComponentDeclaration> param) {
            return new ListCell<>() {
                @Override
                public void updateItem(ModulePipelineComponentDeclaration config, boolean empty) {
                    super.updateItem(config, empty);
                    setText(null);
                    setTooltip(null);
                    if (empty || config == null) {
                        setGraphic(null);
                    } else {
                        VBox cell = new VBox();
                        cell.getChildren().add(new Text(config.getName()));
                        cell.getChildren().add(new Text(config.getClazz().getName()));
                        cell.getChildren().forEach(t -> t.getStyleClass().add("text-display"));
                        cell.getStyleClass().add("component-entry");
                        setGraphic(cell);
                        Tooltip tooltip = new Tooltip();
                        tooltip.setText(config.getDesc());
                        tooltip.setShowDelay(Duration.ZERO);
                        setTooltip(tooltip);
                    }
                }
            };
        }
    }
}

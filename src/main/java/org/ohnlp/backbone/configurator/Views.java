package org.ohnlp.backbone.configurator;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import org.ohnlp.backbone.configurator.gui.controller.PipelineEditorController;
import org.ohnlp.backbone.configurator.gui.controller.WelcomeAndConfigSelectionController;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CancellationException;

public class Views {

    public enum ViewType {
        CONFIG_LIST,
        NEW_PIPELINE,
        PIPELINE_EDITOR,
        COMPONENT_BROWSER,
        COMPONENT_EDITOR
    }

    public static void openView(ViewType type) throws IOException {
        openView(type, null, false);
    }

    public static void openView(ViewType type, Window stage, boolean closeExisting) throws IOException {
        switch (type) {
            case CONFIG_LIST:
                newConfigListView();
                break;
            case NEW_PIPELINE:
                newPipelineView();
                break;
            case PIPELINE_EDITOR:
                newPipelineEditorView();
                break;
            case COMPONENT_BROWSER:
                newComponentBrowserView();
                break;
            case COMPONENT_EDITOR:
                newComponentEditorView();
                break;
        }
        if (closeExisting && stage != null) {
            stage.hide();
        }
    }

    private static void newComponentBrowserView() throws IOException {
        FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-browser-view.fxml"));
        Stage stage = new Stage();
        stage.setTitle("Add Pipeline Component");
        Scene s = new Scene(loader.load());
        s.getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        s.getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/component-browser-view.css").toExternalForm());
        stage.setScene(s);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private static void newConfigListView() throws IOException {
        Stage stage = new Stage();
        FXMLLoader fxmlLoader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/welcome-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("OHNLP Toolkit Pipeline Configuration Editor");
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private static void newPipelineView() throws IOException {
        FXMLLoader loader = new FXMLLoader(WelcomeAndConfigSelectionController.class.getResource("/org/ohnlp/backbone/configurator/new-pipeline-dialog-view.fxml"));
        Stage stage = new Stage();
        stage.setTitle("OHNLP Toolkit Pipeline Configuration Editor: Create new Pipeline");
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        stage.setScene(scene);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private static void newPipelineEditorView() throws IOException {
        FXMLLoader loader = new FXMLLoader(WelcomeAndConfigSelectionController.class.getResource("/org/ohnlp/backbone/configurator/pipeline-editor-view.fxml"));
        Stage stage = new Stage();
        stage.titleProperty().bind(Bindings.createStringBinding(() -> {
            if (EditorRegistry.getConfigMetadata().isNotNull().get()) {
                return "OHNLP Toolkit Pipeline Configuration Editor: " + EditorRegistry.getConfigMetadata().get().getFile().getName();
            } else {
                return "OHNLP Toolkit Pipeline Configuration Editor";
            }
        }, EditorRegistry.getConfigMetadata()));
        Scene s = new Scene(loader.load());
        s.getStylesheets().add(WelcomeAndConfigSelectionController.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        s.getStylesheets().add(WelcomeAndConfigSelectionController.class.getResource("/org/ohnlp/backbone/configurator/pipeline-editor-view.css").toExternalForm());
        stage.setScene(s);
        stage.setMaximized(true);
        stage.initStyle(StageStyle.UNDECORATED);
        stage.show();
    }

    private static void newComponentEditorView() throws IOException {
        // Since component editor can lead to schema re-resolution, display notice to user here first, to be closed later
        Dialog<Boolean> alert = new Dialog();
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setTitle("Resolving Input/Output Schemas");
        alert.setHeaderText("Attempting to Resolve Input/Output Schemas");
        alert.setContentText("Please Wait...");
        alert.getDialogPane().getStyleClass().add("window");
        alert.getDialogPane().getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        alert.show();
        Platform.runLater(() -> {
            FXMLLoader loader = new FXMLLoader(PipelineEditorController.class.getResource("/org/ohnlp/backbone/configurator/component-editor-view.fxml"));
            Stage stage = new Stage();
            stage.setTitle("Edit Pipeline Step");
            Scene s = null;
            try {
                s = new Scene(loader.load());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            s.getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
            stage.setScene(s);
            stage.initStyle(StageStyle.UNDECORATED);
            stage.show();
            alert.setResult(true);
            alert.close();
        });
    }

    public static void displayUncommitedSaveDialog(String type, Runnable okCallback, Runnable resetCallback) throws DialogCancelledException {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.initStyle(StageStyle.UNDECORATED);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("Unsaved Changes");
        alert.setContentText("There are unsaved changes to this $1. Do you wish to save?".replace("$1", type));
        ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType noSave = new ButtonType("Don't Save", ButtonBar.ButtonData.FINISH);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(ok, noSave, cancel);
        alert.getDialogPane().getStylesheets().add(Views.class.getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        Optional<ButtonType> output = alert.showAndWait();
        if (output.isEmpty() || output.get().equals(cancel)) {
            throw new CancellationException();
        }
        if (output.get().equals(ok)) {
            okCallback.run();
        } else if (output.get().equals(noSave)) {
            resetCallback.run();
        }
    }

    public static class DialogCancelledException extends Exception {}
}

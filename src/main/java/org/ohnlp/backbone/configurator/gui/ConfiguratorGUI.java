package org.ohnlp.backbone.configurator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.ohnlp.backbone.api.components.xlang.python.PythonBridge;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.UpdateManager;

import java.io.File;
import java.io.IOException;

public class ConfiguratorGUI extends Application {

    public static void main(String[] args) {
        try {
            UpdateManager.checkForUpdates();
        } catch (IOException ignored) {}
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        PythonBridge.CLEANUP_ENVS_ON_SHUTDOWN = false; // Use cached configurator mode
        ModuleRegistry.registerJavaModules(new File("modules").listFiles());
        ModuleRegistry.registerJavaModules(new File("python_modules").listFiles()); // TODO
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/ohnlp/backbone/configurator/welcome-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.initStyle(StageStyle.UNDECORATED);
        primaryStage.setTitle("OHNLP Toolkit Pipeline Configuration Editor");
        primaryStage.show();
    }
}

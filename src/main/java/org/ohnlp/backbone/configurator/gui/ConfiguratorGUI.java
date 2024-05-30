package org.ohnlp.backbone.configurator.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.beam.repackaged.core.org.apache.commons.lang3.SystemUtils;
import org.ohnlp.backbone.api.components.xlang.python.PythonBridge;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.UpdateManager;

import java.io.File;
import java.io.IOException;
import javax.net.ssl.*;
import java.security.*;
import java.security.cert.X509Certificate;

public class ConfiguratorGUI extends Application {

    public static void main(String... args) {
        if (System.getenv().containsKey("disableCertChecking")) {
            try {
                turnOffSslChecking();
            } catch (NoSuchAlgorithmException | KeyManagementException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            UpdateManager.checkForUpdates();
        } catch (IOException ignored) {}
        if (UpdateManager.RESTART_REQUIRED) {
            System.out.println("Configurator was Updated, Please Re-Launch the Application");
            System.out.println("Press [ENTER] to exit");
            try {
                System.in.read();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.exit(0);
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        PythonBridge.CLEANUP_ENVS_ON_SHUTDOWN = false; // Use cached configurator mode
        ModuleRegistry.registerJavaModules(new File("modules").listFiles());
        ModuleRegistry.registerPythonModules(new File("python_modules").listFiles()); // TODO
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/org/ohnlp/backbone/configurator/welcome-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load());
        scene.getStylesheets().add(getClass().getResource("/org/ohnlp/backbone/configurator/global.css").toExternalForm());
        primaryStage.setScene(scene);
        if (!SystemUtils.IS_OS_MAC_OSX) {
            primaryStage.initStyle(StageStyle.UNDECORATED);
        }
        primaryStage.setTitle("OHNLP Toolkit Pipeline Configuration Editor");
        primaryStage.show();
    }


    private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[]{
            new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers(){
                    return null;
                }
                public void checkClientTrusted( X509Certificate[] certs, String authType ){}
                public void checkServerTrusted( X509Certificate[] certs, String authType ){}
            }
    };

    public static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException {
        // Install the all-trusting trust manager
        final SSLContext sc = SSLContext.getInstance("SSL");
        sc.init( null, UNQUESTIONING_TRUST_MANAGER, null );
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }

    public static void turnOnSslChecking() throws KeyManagementException, NoSuchAlgorithmException {
        // Return it to the initial state (discovered by reflection, now hardcoded)
        SSLContext.getInstance("SSL").init( null, null, null );
    }

}

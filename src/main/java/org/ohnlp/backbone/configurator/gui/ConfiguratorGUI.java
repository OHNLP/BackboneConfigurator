package org.ohnlp.backbone.configurator.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fxgraph.graph.Graph;
import com.fxgraph.layout.AbegoTreeLayout;
import com.fxgraph.layout.RandomLayout;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.abego.treelayout.Configuration;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.gui.utils.DAGUtils;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfiguratorGUI extends Application {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        ModuleRegistry.registerFiles(new File("modules").listFiles());
        BackboneConfiguration config = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(ModuleRegistry.getComponentClassLoader())).readValue(new File("configs/fh_nlp_biobank_to_csv.json"), BackboneConfiguration.class);
        EditablePipeline pipeline = EditablePipeline.fromConfig(config);
        Graph g = DAGUtils.generateGraphForPipeline(pipeline);
        g.layout(new AbegoTreeLayout(150, 500, Configuration.Location.Top));
        primaryStage.setScene(new Scene(new BorderPane(g.getCanvas())));
        primaryStage.setMaximized(true);
        primaryStage.show();
    }
}

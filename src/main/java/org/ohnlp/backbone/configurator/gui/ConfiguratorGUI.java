package org.ohnlp.backbone.configurator.gui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import javafx.application.Application;
import javafx.stage.Stage;
import org.ohnlp.backbone.api.config.BackboneConfiguration;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class ConfiguratorGUI extends Application {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        ModuleRegistry.registerFiles(new File("modules").listFiles());
        BackboneConfiguration config = new ObjectMapper().setTypeFactory(TypeFactory.defaultInstance().withClassLoader(ModuleRegistry.getComponentClassLoader())).readValue(new File("configs/fh_nlp_biobank_to_csv.json"), BackboneConfiguration.class);
        EditablePipeline pipeline = EditablePipeline.fromConfig(config);
        List<List<PipelineComponentDeclaration>> steps = pipeline.getPipelineAsSteps();

        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {

    }
}

package org.ohnlp.backbone.configurator.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;
import org.springframework.util.Assert;

import java.lang.reflect.Constructor;
import java.util.*;

public class ComponentEditorController {
    @FXML
    public VBox configList;
    @FXML
    public AnchorPane container;

    private Map<String, StringProperty> boundInputs = new HashMap<>();

    @FXML
    public void initialize() {
        Assert.notNull(EditorRegistry.getCurrentEditedComponent(), "Component Editor Dialog Initialized with Null Component");
        SimpleObjectProperty<PipelineComponentDeclaration> edited = EditorRegistry.getCurrentEditedComponent();
        configList.prefWidthProperty().bind(container.widthProperty());
        TitledPane stepIDPrompt = new TitledPane("Step ID", new TextField(edited.get().getComponentID()));
        configList.getChildren().add(stepIDPrompt);
        // TODO generate inputs prompt
        generateInputs(edited.get());
        edited.get().getConfig().forEach(f -> {
            TitledPane p = new TitledPane();
            p.setText(f.getDesc());
            p.setContent(f.getImpl().render(Arrays.asList( // TODO how do we generate this?
            )));
            configList.getChildren().add(p);
        });
    }

    private void generateInputs(PipelineComponentDeclaration componentDec) {
        EditablePipeline pipeline = EditorRegistry.getCurrentEditablePipeline().get();
        Set<String> possibleInputs = pipeline.getAvailableInputs(componentDec);
        BackbonePipelineComponent cmp = initComponent(componentDec);
        if (cmp instanceof HasInputs) {
            boundInputs.clear();;
            TitledPane inputPrompt = new TitledPane();
            inputPrompt.setText("Input Steps");
            VBox out = new VBox();
            ((HasInputs) cmp).getInputTags().forEach(tag -> {
                HBox inputRow = new HBox();
                Text label = new Text(tag + " From: ");
                ComboBox<String> inputComponentID = new ComboBox<>();
                inputComponentID.setItems(FXCollections.observableArrayList(possibleInputs).sorted());
                ComboBox<String> inputComponentTag = new ComboBox<>();
                inputComponentTag.itemsProperty().bind(Bindings.createObjectBinding(() -> {
                    if (inputComponentID.valueProperty().isNotNull().get()) {
                        PipelineComponentDeclaration v = pipeline.getComponentByID(inputComponentID.valueProperty().get());
                        if (v != null) {
                            BackbonePipelineComponent srcComponent = initComponent(v);
                            if (srcComponent instanceof HasOutputs) {
                                return FXCollections.observableArrayList(((HasOutputs) srcComponent).getOutputTags());
                            }
                        }
                    }
                    return FXCollections.emptyObservableList();

                }, inputComponentID.valueProperty()));
                inputRow.getChildren().addAll(label, inputComponentID, inputComponentTag);
                String outputID = null;
                String outputTag = null;
                if (componentDec.getInputs().containsKey(tag)) {
                    outputID = componentDec.getInputs().get(tag).getComponentID();
                    outputTag = componentDec.getInputs().get(tag).getInputTag();
                } else if (((HasInputs) cmp).getInputTags().size() == 1 && componentDec.getInputs().size() == 1) {
                    BackbonePipelineComponentConfiguration.InputDefinition inputDef = componentDec.getInputs().values().stream().findFirst().get();
                    outputID = inputDef.getComponentID();
                    outputTag = inputDef.getInputTag();
                }
                if (outputID != null) {
                    inputComponentID.getSelectionModel().select(inputComponentID.getItems().indexOf(outputID));
                    if (outputTag.equals("*")) {
                        outputTag = inputComponentTag.getItems().size() > 0 ? inputComponentTag.getItems().get(0) : null;
                    }
                    inputComponentTag.getSelectionModel().select(inputComponentTag.getItems().indexOf(outputTag));
                }
                out.getChildren().add(inputRow);
            });
            inputPrompt.setContent(out);
            configList.getChildren().add(inputPrompt);
        }

    }

    @FXML
    public void onReset(ActionEvent e) {
        EditorRegistry.getCurrentEditedComponent().get().getConfig().forEach(f -> {
            f.getImpl().reset();
        });
        configList.getChildren().clear();
        initialize();
    }

    private static BackbonePipelineComponent initComponent(PipelineComponentDeclaration component) {
        try {
            Constructor<? extends BackbonePipelineComponent> ctor =
                    component.getComponentDef().getClazz().getDeclaredConstructor();
            return ctor.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}

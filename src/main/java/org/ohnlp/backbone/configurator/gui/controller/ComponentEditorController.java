package org.ohnlp.backbone.configurator.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
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

    private Map<String, ObjectBinding<BackbonePipelineComponentConfiguration.InputDefinition>> boundInputs = new HashMap<>();

    @FXML
    public void initialize() {
        Assert.notNull(EditorRegistry.getCurrentEditedComponent(), "Component Editor Dialog Initialized with Null Component");
        SimpleObjectProperty<PipelineComponentDeclaration> edited = EditorRegistry.getCurrentEditedComponent();
        configList.prefWidthProperty().bind(container.widthProperty());
        TitledPane stepIDPrompt = new TitledPane("Step ID (Required)", new TextField(edited.get().getComponentID()));
        configList.getChildren().add(stepIDPrompt);
        // TODO generate inputs prompt
        generateInputs(edited.get());
        edited.get().getConfig().forEach(f -> {
            TitledPane p = new TitledPane();
            String title = f.getDesc();
            if (f.isRequired()) {
                title += " (Required)";
            }
            p.setText(title);
            p.setContent(f.getImpl().render(new HashMap<>())); // TODO how do we generate this
            configList.getChildren().add(p);
        });
    }

    private void generateInputs(PipelineComponentDeclaration componentDec) {
        EditablePipeline pipeline = EditorRegistry.getCurrentEditablePipeline().get();
        Set<String> possibleInputs = pipeline.getAvailableInputs(componentDec);
        BackbonePipelineComponent cmp = initComponent(componentDec);
        if (cmp instanceof HasInputs) {
            boundInputs.clear();
            TitledPane inputPrompt = new TitledPane();
            inputPrompt.setText("Input Steps");
            VBox out = new VBox();
            ((HasInputs) cmp).getInputTags().forEach(tag -> {
                // Render input row and generate available output collections for this tag
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
                // Add bound property to keep track of values
                ObjectBinding<BackbonePipelineComponentConfiguration.InputDefinition> def = Bindings.createObjectBinding(
                        () -> {
                            if (inputComponentID.valueProperty().isNotNull().and(inputComponentTag.valueProperty().isNotNull()).get()) {
                                BackbonePipelineComponentConfiguration.InputDefinition d = new BackbonePipelineComponentConfiguration.InputDefinition();
                                d.setComponentID(inputComponentID.getValue());
                                d.setInputTag(inputComponentTag.getValue());
                                return d;
                            } else {
                                return null;
                            }
                        }, inputComponentID.valueProperty(), inputComponentTag.valueProperty()
                );
                boundInputs.put(tag, def);

                // Load Pre-existing values
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

    @FXML
    public void onCommit(ActionEvent e) {
        EditorRegistry.getCurrentEditedComponent().get().getConfig().forEach(f -> {
            f.getImpl().commit();
        });
        HashMap<String, BackbonePipelineComponentConfiguration.InputDefinition> inputs = convertBoundInputsToUnbound();
        EditorRegistry.getCurrentEditedComponent().get().setInputs(inputs);
    }

    private HashMap<String, BackbonePipelineComponentConfiguration.InputDefinition> convertBoundInputsToUnbound() {
        HashMap<String, BackbonePipelineComponentConfiguration.InputDefinition> ret = new HashMap<>();
        boundInputs.forEach((tag, def) -> {
            if (def.isNotNull().get()) {
                ret.put(tag, def.getValue());
            }
        });
        return ret;
    }

    @FXML
    public void onClose(ActionEvent actionEvent) {
        boolean promptSave = EditorRegistry.getCurrentEditedComponent().get().getConfig().stream().map(f -> f.getImpl().isDirty()).reduce((b1, b2) -> b1 || b2).orElse(false);
        promptSave = promptSave || !convertBoundInputsToUnbound().equals(EditorRegistry.getCurrentEditedComponent().get().getInputs());
        if (promptSave) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Unsaved Changes");
            alert.setHeaderText("Unsaved Changes");
            alert.setContentText("There are unsaved changes to this module configuration. Do you wish to save?");
            ButtonType ok = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
            ButtonType noSave = new ButtonType("Don't Save", ButtonBar.ButtonData.FINISH);
            ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(ok, noSave, cancel);
            Optional<ButtonType> output = alert.showAndWait();
            if (output.isEmpty()) {
                return;
            }
            if (output.get().equals(cancel)) {
                return;
            }
            if (output.get().equals(ok)) {
                onCommit(actionEvent);
            } else if (output.get().equals(noSave)) {
                onReset(actionEvent);
            }
        }
        ((Node)actionEvent.getSource()).getScene().getWindow().hide();

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

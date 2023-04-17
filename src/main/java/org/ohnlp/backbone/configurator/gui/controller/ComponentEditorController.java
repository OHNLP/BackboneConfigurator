package org.ohnlp.backbone.configurator.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleMapProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.StageStyle;
import org.apache.beam.sdk.schemas.Schema;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.Views;
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
    @FXML
    public VBox contents;

    private Map<String, ObjectBinding<BackbonePipelineComponentConfiguration.InputDefinition>> boundInputs = new HashMap<>();
    private StringProperty stepIDProperty;
    private ObservableMap<String, Schema> inputSchemas = FXCollections.observableHashMap();

    @FXML
    public void initialize() {
        contents.prefHeightProperty().bind(container.heightProperty());
        Assert.notNull(EditorRegistry.getCurrentEditedComponent(), "Component Editor Dialog Initialized with Null Component");
        EditorRegistry.getCurrentEditedComponent().get().setUpdateOutputSchemas(true);
        SimpleObjectProperty<PipelineComponentDeclaration> edited = EditorRegistry.getCurrentEditedComponent();
        configList.prefWidthProperty().bind(container.widthProperty());
        TextField stepIDPrompt = new TextField(edited.get().getComponentID());
        configList.getChildren().add(new TitledPane("Step ID (Required)", stepIDPrompt));
        this.stepIDProperty = stepIDPrompt.textProperty();
        generateInputs(edited.get());
        edited.get().getConfig().forEach(f -> {
            TitledPane p = new TitledPane();
            String title = f.getDesc();
            if (f.isRequired()) {
                title += " (Required)";
            }
            p.setText(title);
            p.setContent(f.getImpl().render(inputSchemas));
            configList.getChildren().add(p);
        });
    }

    private void generateInputs(PipelineComponentDeclaration componentDec) {
        EditablePipeline pipeline = EditorRegistry.getCurrentEditablePipeline().get();
        Set<String> possibleInputs = pipeline.getAvailableInputs(componentDec);
        BackbonePipelineComponent<?,?> cmp = componentDec.componentInstance(false);
        if (cmp instanceof HasInputs) {
            boundInputs.clear();
            TitledPane inputPrompt = new TitledPane();
            inputPrompt.setText("Input Steps");
            VBox out = new VBox();
            ((HasInputs) cmp).getInputTags().forEach(tag -> {
                // Render input row and generate available output collections for this tag
                HBox inputRow = new HBox();
                inputRow.setAlignment(Pos.CENTER_LEFT);
                inputRow.setSpacing(5);
                Text label = new Text(tag + " From:");
                ComboBox<String> inputComponentID = new ComboBox<>();
                inputComponentID.setItems(FXCollections.observableArrayList(possibleInputs).sorted());
                ComboBox<String> inputComponentTag = new ComboBox<>();
                inputComponentTag.itemsProperty().bind(Bindings.createObjectBinding(() -> {
                    if (inputComponentID.valueProperty().isNotNull().get()) {
                        PipelineComponentDeclaration v = pipeline.getComponentByID(inputComponentID.valueProperty().get());
                        if (v != null) {
                            BackbonePipelineComponent<?,?> srcComponent = v.componentInstance(false);
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
                // Update the available input map on change
                def.addListener((e, o, n) -> {
                    if (n == null || pipeline.getComponentByID(n.getComponentID()) == null) {
                        inputSchemas.put(tag, Schema.of());
                    } else {
                        PipelineComponentDeclaration srcComponent = pipeline.getComponentByID(n.getComponentID());
                        inputSchemas.put(tag, srcComponent.getStepOutput().getOrDefault(n.getInputTag(), Schema.of()));
                    }
                });

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
        PipelineComponentDeclaration currComponent = EditorRegistry.getCurrentEditedComponent().get();
        boolean changed = currComponent.getConfig().stream().map(f -> f.getImpl().commit()).reduce((b1, b2) -> b1 || b2).orElse(false);
        HashMap<String, BackbonePipelineComponentConfiguration.InputDefinition> inputs = convertBoundInputsToUnbound();
        currComponent.setInputs(inputs);
        // Renaming step ID is a bit more involved as we have to check for input declarations as well
        if (EditorRegistry.getCurrentEditablePipeline().get().getComponentByID(currComponent.getComponentID()) != null) {
            EditorRegistry.getCurrentEditablePipeline().get().renameComponent(currComponent.getComponentID(), stepIDProperty.getValue());
        } else {
            currComponent.setComponentID(stepIDProperty.getValue());
        }
        // If creating new component, ensure this is reflected in edited pipeline
        if (EditorRegistry.inCreateNewComponentState().get()) {
            EditorRegistry.getCurrentEditablePipeline().get().addComponent(currComponent);
            EditorRegistry.inCreateNewComponentState().set(false);
        }
        if (changed) {
            EditorRegistry.getCurrentEditablePipeline().get().dirtyProperty().set(true);
        }
        // Now indicate that relns etc. might have changed and we might need to redraw the graph
        EditorRegistry.refreshGraphProperty().set(true);
        container.getScene().getWindow().hide();
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
        if (stepIDProperty.isNotNull().get()) {
            promptSave = promptSave || !stepIDProperty.getValue().equals(EditorRegistry.getCurrentEditedComponent().get().getComponentID());
        }
        if (promptSave) {
            try {
                Views.displayUncommitedSaveDialog("module configuration", () -> onCommit(actionEvent), () -> onReset(actionEvent));
            } catch (Views.DialogCancelledException e) {
                return;
            }
        }
        ((Node)actionEvent.getSource()).getScene().getWindow().hide();

    }
}

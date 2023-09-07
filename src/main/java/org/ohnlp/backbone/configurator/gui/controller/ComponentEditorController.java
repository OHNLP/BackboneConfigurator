package org.ohnlp.backbone.configurator.gui.controller;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
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

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class ComponentEditorController {

    public static final SimpleObjectProperty<ComponentEditorController> CURR_COMPONENT_EDITOR = new SimpleObjectProperty<>();
    public static final SimpleBooleanProperty COMPONENT_EDITOR_EXIT_FLAG = new SimpleBooleanProperty(false);

    static {
        // bind window close to exit flag
        COMPONENT_EDITOR_EXIT_FLAG.addListener((o, e, n) -> {
            if (n) {
                if (CURR_COMPONENT_EDITOR.isNotNull().get()) {
                    CURR_COMPONENT_EDITOR.get().container.getScene().getWindow().hide();
                    CURR_COMPONENT_EDITOR.set(null);
                }
                COMPONENT_EDITOR_EXIT_FLAG.set(false);
            }
        });
    }

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
        if (!EditorRegistry.inCreateNewComponentState().get()) {
            EditorRegistry.getCurrentEditedComponent().get().setUpdateOutputSchemas(true);
        }
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
        CURR_COMPONENT_EDITOR.set(this);
    }

    private void generateInputs(PipelineComponentDeclaration componentDec) {
        Dialog<Boolean> instanceInitDialog = Views.createSyncDialog(new SimpleStringProperty("Loading Component"), new SimpleStringProperty("Loading Component and Environment"), new SimpleStringProperty("Please Wait..."));
        instanceInitDialog.show();
        EditablePipeline pipeline = EditorRegistry.getCurrentEditablePipeline().get();
        Set<String> possibleInputs = pipeline.getAvailableInputs(componentDec);
        CompletableFuture<? extends BackbonePipelineComponent<?, ?>> future = componentDec.getComponentDef().getInstance(componentDec, false);
        future.whenComplete((cmp, err) -> {
            instanceInitDialog.setResult(true);
            instanceInitDialog.close();
            if (err != null) {
                err.printStackTrace();
                try {
                    Views.displayConfirmationDialog("Failed to load Component", "Component Loading Failed, Please check console for error log", () -> {}, null);
                } catch (Views.DialogCancelledException e) {
                    return;
                }
                return;
            }
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
                    Text label = tag.equals("*") ? new Text("Any Input From: ") : new Text(tag + " From:");
                    ComboBox<String> inputComponentID = new ComboBox<>();
                    inputComponentID.setItems(FXCollections.observableArrayList(possibleInputs).sorted());
                    ComboBox<String> inputComponentTag = new ComboBox<>();
                    inputComponentTag.itemsProperty().bind(Bindings.createObjectBinding(() -> {
                        if (inputComponentID.valueProperty().isNotNull().get()) {
                            PipelineComponentDeclaration v = pipeline.getComponentByID(inputComponentID.valueProperty().get());
                            if (v != null) {
                                BackbonePipelineComponent<?,?> srcComponent = v.getComponentDef().getInstance(v, false).get();
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
        });
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
        CURR_COMPONENT_EDITOR.set(null);
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
        if (!checkForSave(true)) {
            return;
        }
        CURR_COMPONENT_EDITOR.set(null);
        container.getScene().getWindow().hide();
    }

    private boolean checkForSave(boolean allowCancellable) {
        boolean promptSave = EditorRegistry.getCurrentEditedComponent().get().getConfig().stream().map(f -> f.getImpl().isDirty()).reduce((b1, b2) -> b1 || b2).orElse(false);
        promptSave = promptSave || !convertBoundInputsToUnbound().equals(EditorRegistry.getCurrentEditedComponent().get().getInputs());
        if (stepIDProperty.isNotNull().get()) {
            promptSave = promptSave || !stepIDProperty.getValue().equals(EditorRegistry.getCurrentEditedComponent().get().getComponentID());
        }
        if (promptSave) {
            try {
                Views.displayUncommitedSaveDialog("module configuration", allowCancellable, () -> onCommit(null), () -> onReset(null));
                return true;
            } catch (Views.DialogCancelledException e) {
                if (!allowCancellable) {
                    onReset(null);
                }
                return false;
            }
        }
        return true;
    }
}

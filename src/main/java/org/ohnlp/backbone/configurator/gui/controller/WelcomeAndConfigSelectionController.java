package org.ohnlp.backbone.configurator.gui.controller;

import javafx.beans.binding.Bindings;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import org.ohnlp.backbone.configurator.ConfigManager;
import org.ohnlp.backbone.configurator.EditorRegistry;
import org.ohnlp.backbone.configurator.Views;
import org.ohnlp.backbone.configurator.gui.components.ConfigListContextMenu;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class WelcomeAndConfigSelectionController {
    @FXML
    public ListView<ConfigManager.ConfigMeta> activeConfigList;

    @FXML
    public TextField searchFilter;

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    @FXML
    public void initialize() {
        // Bind config list to search filter
        FilteredList<ConfigManager.ConfigMeta> filteredConfigs = ConfigManager.getConfigs().filtered(null);
        filteredConfigs.predicateProperty().bind(Bindings.createObjectBinding(() -> {
            String txt = searchFilter.getText();
            if (txt == null || txt.trim().length() == 0) {
                return null;
            } else {
                String filter = txt.trim().toLowerCase(Locale.ROOT);
                return (config) -> {
                    String desc = "No description saved";
                    if (config.getDesc() != null) {
                        desc = config.getDesc();
                    }
                    String searchAgainst = String.join(" ", config.getName(), config.getFile().getName(), desc);
                    return searchAgainst.toLowerCase(Locale.ROOT).contains(filter);
                };
            }
        }, searchFilter.textProperty()));
        // Populate config list
        activeConfigList.setItems(filteredConfigs);
        activeConfigList.setCellFactory(new ConfigMetaCellFactory());
        // Set growths for display/fill
        HBox.setHgrow(searchFilter, Priority.ALWAYS);
    }

    @FXML
    public void onOpenSelectedPipeline(ActionEvent event) {
        ConfigManager.ConfigMeta active = activeConfigList.getSelectionModel().getSelectedItem();
        if (active == null) {
            return;
        }
        try {
            EditorRegistry.setCurrentConfig(active);
            Views.openView(Views.ViewType.PIPELINE_EDITOR, ((Node)event.getSource()).getScene().getWindow(), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @FXML
    public void createNewPipeline(ActionEvent event) {
        try {
            Views.openView(Views.ViewType.NEW_PIPELINE);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public class ConfigMetaCellFactory implements Callback<ListView<ConfigManager.ConfigMeta>, ListCell<ConfigManager.ConfigMeta>> {

        @Override
        public ListCell<ConfigManager.ConfigMeta> call(ListView<ConfigManager.ConfigMeta> param) {
            return new ListCell<>() {
                @Override
                public void updateItem(ConfigManager.ConfigMeta config, boolean empty) {
                    super.updateItem(config, empty);
                    setText(null);
                    if (empty || config == null) {
                        setGraphic(null);
                    } else {
                        Label wrapper = new Label();
                        VBox node = new VBox();
                        HBox main = new HBox();
                        String desc = "No description saved";
                        if (config.getDesc() != null) {
                            desc = config.getDesc();
                        }
                        Text label = new Text(config.getName() + "\r\n" + config.getFile().getName());
                        main.getChildren().add(label);
                        Region fill = new Region();
                        HBox.setHgrow(fill, Priority.ALWAYS);
                        main.getChildren().add(fill);
                        Text lm = new Text(sdf.format(new Date(config.getLastModified().toEpochMilli())));
                        lm.setTextAlignment(TextAlignment.RIGHT);
                        main.getChildren().add(lm);
                        main.setAlignment(Pos.TOP_RIGHT);
                        node.getChildren().addAll(main, new Text(desc));
                        wrapper.setGraphic(node);
                        Pane parent = new Pane(wrapper); // Control cannot be top level graphic as it will mess with widths
                        setGraphic(parent);
                        ConfigListContextMenu ctx = new ConfigListContextMenu(config);
                        wrapper.setContextMenu(ctx);
                        wrapper.prefWidthProperty().bind(parent.widthProperty());
                    }
                }
            };
        }
    }
}

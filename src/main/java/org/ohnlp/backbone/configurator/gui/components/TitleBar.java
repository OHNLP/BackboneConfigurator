package org.ohnlp.backbone.configurator.gui.components;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

public class TitleBar extends HBox {

    @FXML
    private BooleanProperty maximizable = new SimpleBooleanProperty(true);

    public TitleBar() {
        super();
        getStyleClass().add("title-bar");
        setAlignment(Pos.CENTER_LEFT);
        // Populate contents and initialize buttons/layouts
        Text title = new Text();
        Platform.runLater(() -> title.setText(((Stage) getScene().getWindow()).getTitle())); // Scene is null on init, so we delay-populate this
        Pane fill = new Pane();
        Button minimize = new Button();
        FontIcon minimizeIcon = new FontIcon("far-window-minimize");
        minimizeIcon.setIconColor(Color.GRAY);
        minimize.setGraphic(minimizeIcon);
        minimize.getStyleClass().add("title-button");
        Pane maxRestoreWrapper = new Pane();
        Button maxRestore = new Button();
        FontIcon maxRestoreIcon = new FontIcon("far-window-maximize");
        Platform.runLater(() -> {
            if (((Stage) getScene().getWindow()).isMaximized()) {
                maxRestoreIcon.setIconLiteral("far-window-restore");
            } else {
                maxRestoreIcon.setIconLiteral("far-window-maximize");
            }
            ((Stage) getScene().getWindow()).maximizedProperty().addListener((e, o, n) -> {
                if (n != null && n) {
                    maxRestoreIcon.setIconLiteral("far-window-restore");
                } else {
                    maxRestoreIcon.setIconLiteral("far-window-maximize");
                }
            });
        });
        maxRestoreIcon.setIconColor(Color.GRAY);
        maxRestore.getStyleClass().add("title-button");
        maxRestore.setGraphic(maxRestoreIcon);
        if (maximizable.get()) {
            maxRestoreWrapper.getChildren().add(maxRestore);
        }
        maximizable.addListener((e, o, n) -> {
            maxRestoreWrapper.getChildren().clear();
            if (n) {
                maxRestoreWrapper.getChildren().add(maxRestore);
            }
        });
        Button close = new Button();
        FontIcon closeIcon = new FontIcon("fas-times");
        closeIcon.setIconSize(18);
        closeIcon.setIconColor(Color.GRAY);
        close.getStyleClass().add("title-close-button");
        close.setGraphic(closeIcon);

        close.getStyleClass().add("title-button");
        getChildren().addAll(title, fill, minimize, maxRestoreWrapper, close);
        HBox.setHgrow(fill, Priority.ALWAYS);
        paddingProperty().set(new Insets(0, 0, 0, 10));

        // Make draggable
        final Delta drag = new Delta();
        this.setOnMousePressed(e -> {
            Window stage = ((Node) e.getSource()).getScene().getWindow();
            drag.x = stage.getX() - e.getScreenX();
            drag.y = stage.getY() - e.getScreenY();
        });
        this.setOnMouseDragged(e -> {
            Window stage = ((Node) e.getSource()).getScene().getWindow();
            stage.setX(e.getScreenX() + drag.x);
            stage.setY(e.getScreenY() + drag.y);
        });

        // Bind button actions
        minimize.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                ((Stage) ((Node) e.getSource()).getScene().getWindow()).setIconified(true);
            }
        });

        maxRestore.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                Stage s = ((Stage) ((Node) e.getSource()).getScene().getWindow());
                s.setMaximized(!s.isMaximized());
            }
        });

        close.setOnMouseClicked(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                ((Node)e.getSource()).getScene().getWindow().hide();
            }
        });
    }

    private static class Delta {
        double x, y;
    }

    public boolean isMaximizable() {
        return maximizable.get();
    }

    public BooleanProperty maximizableProperty() {
        return maximizable;
    }

    public void setMaximizable(boolean maximizable) {
        this.maximizable.set(maximizable);
    }
}

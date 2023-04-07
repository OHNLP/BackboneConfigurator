package org.ohnlp.backbone.configurator.gui.components.config;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

public abstract class RenderableConfigurationField {

    private String label;
    private String desc;
    private boolean required;

    public RenderableConfigurationField(String label, String desc, boolean required) {
        this.label = label;
        this.desc = desc;
        this.required = required;
    }

    public Node getLabel() {
        HBox wrapper = new HBox();
        wrapper.getChildren().add(new Text(label));
        if (required) {
            Text requiredLabel = new Text("*");
            requiredLabel.setTranslateY(requiredLabel.getFont().getSize() * -0.3);
            requiredLabel.setStyle("-fx-text-fill: red");
        }
        return wrapper;
    }

    public Node getDescriptionOverlay() {
        // TODO
        return null;
    }


}

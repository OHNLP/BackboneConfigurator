package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.shape.Line;

public class DirectedEdgeFromToLabel extends Edge {
    public final String srcLabel;
    public final String tgtLabel;
    public final ComponentCell src;
    public final ComponentCell tgt;

    public DirectedEdgeFromToLabel(ComponentCell source, ComponentCell target, String srcLabel, String tgtLabel) {
        super(source, target);
        this.src = target; // Invert because parent/child is inverted in inherited class
        this.tgt = source;
        this.srcLabel = srcLabel;
        this.tgtLabel = tgtLabel;
        if (src.getOutputs().size() > 1 && !srcLabel.equalsIgnoreCase("*")) {
            int idx = src.getOutputs().indexOf(srcLabel);
            tgt.setSrcOutputIndex(idx);
        }
        if (tgt.getInputs().size() > 1 && !tgtLabel.equalsIgnoreCase("*")) {
            int idx = tgt.getInputs().indexOf(tgtLabel);
            tgt.setInputIdx(idx);
        }
    }

    @Override
    public EdgeGraphic getGraphic(Graph graph) {
        return new DirectedEdgeGraphicWithLabel(graph, this, this.textProperty());
    }

    public class DirectedEdgeGraphicWithLabel extends EdgeGraphic {

        private final DirectedEdgeFromToLabel edge;

        public DirectedEdgeGraphicWithLabel(Graph graph, DirectedEdgeFromToLabel edge, StringProperty textProperty) {
            super(graph, edge, textProperty);
            this.edge = edge;
            // Re-calculate source/target y. Anchors start at center of node
            this.getLine().startYProperty().bind(tgt.getYAnchor(graph, edge).subtract(40));
            this.getLine().endYProperty().bind(src.getYAnchor(graph, edge).add(40));
            // Now recalculate source/target x
            if (src.getOutputs().size() > 1 && !srcLabel.equalsIgnoreCase("*")) {
                double width = 250./src.getOutputs().size();
                int idx = src.getOutputs().indexOf(srcLabel);
                tgt.setSrcOutputIndex(idx);
                this.getLine().endXProperty().bind(src.getXAnchor(graph, edge).subtract(125).subtract(width/2.).add(width * (idx + 1)));
            }
            if (tgt.getInputs().size() > 1 && !tgtLabel.equalsIgnoreCase("*")) {
                double width = 250./tgt.getOutputs().size();
                int idx = tgt.getInputs().indexOf(tgtLabel);
                tgt.setInputIdx(idx);
                this.getLine().startXProperty().bind(tgt.getXAnchor(graph, edge).subtract(125).add(width * (idx + 1)));
            }
            // Draw arrow for directionality
            DoubleProperty startX = getLine().startXProperty();
            DoubleProperty endX = getLine().endXProperty();
            DoubleProperty startY = getLine().startYProperty();
            DoubleProperty endY = getLine().endYProperty();
            DoubleBinding dx = endX.subtract(startX);
            DoubleBinding dy = endY.subtract(startY);
            // Use "50" as an approximation for infinite slope so other calculations can still be done without requiring a whole separate set of bindings
            DoubleBinding slope = Bindings.createDoubleBinding(() -> dx.isEqualTo(0).get() ? 50 : dy.divide(dx).get(), dy, dx); ;
            DoubleBinding lineAngle = Bindings.createDoubleBinding(() -> Math.atan(slope.get()), slope);
            DoubleBinding arrowAngle = Bindings.createDoubleBinding(() -> endX.greaterThan(startX.get()).get() ?  Math.toRadians(10) : -Math.toRadians(190), endX, startX);
            DoubleBinding arrowLength = Bindings.createDoubleBinding(() -> 15d);

            Line arrow1 = new Line();
            arrow1.startXProperty().bind(startX);
            arrow1.startYProperty().bind(startY);
            arrow1.endXProperty().bind(startX.add(arrowLength.multiply(
                    Bindings.createDoubleBinding(() -> Math.cos(lineAngle.subtract(arrowAngle).get()), lineAngle, arrowAngle))));
            arrow1.endYProperty().bind(startY.add(arrowLength.multiply(
                    Bindings.createDoubleBinding(() -> Math.sin(lineAngle.subtract(arrowAngle).get()), lineAngle, arrowAngle))));

            Line arrow2 = new Line();
            arrow2.startXProperty().bind(startX);
            arrow2.startYProperty().bind(startY);
            arrow2.endXProperty().bind(startX.add(arrowLength.multiply(
                    Bindings.createDoubleBinding(() -> Math.cos(lineAngle.add(arrowAngle).get()), lineAngle, arrowAngle))));
            arrow2.endYProperty().bind(startY.add(arrowLength.multiply(
                    Bindings.createDoubleBinding(() -> Math.sin(lineAngle.add(arrowAngle).get()), lineAngle, arrowAngle))));
            this.getGroup().getChildren().addAll(arrow1, arrow2);
        }
    }
}

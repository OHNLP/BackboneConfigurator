package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import javafx.beans.property.StringProperty;

public class DirectedEdgeFromToLabel extends Edge {
    public final String srcLabel;
    public final String tgtLabel;
    public final ComponentCell src;
    public final ComponentCell tgt;

    public DirectedEdgeFromToLabel(ComponentCell source, ComponentCell target, String srcLabel, String tgtLabel) {
        super(source, target);
        this.src = target;
        this.tgt = source;
        this.srcLabel = srcLabel;
        this.tgtLabel = tgtLabel;
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
                this.getLine().endXProperty().bind(src.getXAnchor(graph, edge).subtract(125).subtract(width/2.).add(width * (idx + 1)));
            }
            if (tgt.getInputs().size() > 1 && !tgtLabel.equalsIgnoreCase("*")) {
                double width = 250./tgt.getOutputs().size();
                int idx = tgt.getInputs().indexOf(tgtLabel);
                this.getLine().startXProperty().bind(tgt.getXAnchor(graph, edge).subtract(125).add(width * (idx + 1)));
            }
        }
    }
}

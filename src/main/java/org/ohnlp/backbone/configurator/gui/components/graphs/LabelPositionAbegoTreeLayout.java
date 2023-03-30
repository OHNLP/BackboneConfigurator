package org.ohnlp.backbone.configurator.gui.components.graphs;

import com.fxgraph.graph.ICell;
import com.fxgraph.layout.AbegoTreeLayout;
import org.abego.treelayout.Configuration;
import org.abego.treelayout.util.DefaultTreeForTreeLayout;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class LabelPositionAbegoTreeLayout extends AbegoTreeLayout {

    public LabelPositionAbegoTreeLayout(double gapBetweenLevels, double gapBetweenNodes, Configuration.Location location) {
        super(gapBetweenLevels, gapBetweenNodes, location);
    }


    public void addRecursively(DefaultTreeForTreeLayout<ICell> layout, ICell node) {
        List<ICell> toProcess = new ArrayList<>();
        if (node instanceof ComponentCell) {
            ComponentCell srcComponent = (ComponentCell) node;
            List<ComponentCell> children = node.getCellChildren().stream().map(s -> ((ComponentCell) s)).collect(Collectors.toList());
            if (srcComponent.getOutputs().size() > 1) {
                children.sort(Comparator.comparingInt(ComponentCell::getSrcOutputIndex).thenComparingInt(ComponentCell::getInputIdx));
            }
            toProcess.addAll(children);
        } else {
            toProcess = node.getCellChildren();
        }
        toProcess.forEach(cell -> {
            if(!layout.hasNode(cell)) {
                layout.addChild(node, cell);
                addRecursively(layout, cell);
            }
        });
    }
}

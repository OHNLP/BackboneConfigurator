package org.ohnlp.backbone.configurator.gui.utils;

import com.fxgraph.edges.CorneredEdge;
import com.fxgraph.edges.DoubleCorneredEdge;
import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.graph.Model;
import javafx.geometry.Orientation;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.configurator.gui.components.graphs.ComponentCell;
import org.ohnlp.backbone.configurator.gui.components.graphs.DirectedEdgeFromToLabel;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAGUtils {
    public static Graph generateGraphForPipeline(EditablePipeline pipeline) {
        List<List<PipelineComponentDeclaration>> pipelineComponents = pipeline.getPipelineAsSteps();
        Graph outputGraph = new Graph();
        Model model = outputGraph.getModel();
        Map<String, PipelineComponentDeclaration> componentsByID = new HashMap<>();
        Map<String, ComponentCell> cellsByID = new HashMap<>();
        // Draw
        pipelineComponents.forEach(step -> {
            step.forEach(component -> {
                BackbonePipelineComponent componentInstance = initComponent(component);
                ComponentCell cell = new ComponentCell(
                        component.getComponentID(),
                        component.getComponentDef().getName(),
                        component.getComponentDef().getDesc(),
                        componentInstance instanceof HasInputs ? ((HasInputs)componentInstance).getInputTags() : Collections.emptyList(),
                        componentInstance instanceof HasOutputs ? ((HasOutputs)componentInstance).getOutputTags() : Collections.emptyList(),
                        componentInstance);
                componentsByID.put(component.getComponentID(), component);
                cellsByID.put(component.getComponentID(), cell);
                model.addCell(cell);
            });
        });
        // Now make linkages
        componentsByID.forEach((id, comp) -> {
            ComponentCell tgtCell = cellsByID.get(id);
            if (comp.getInputs() != null) {
                comp.getInputs().forEach((tag, src) -> {
                    String sourceID = src.getComponentID();
                    ComponentCell srcCell = cellsByID.get(sourceID);
                    if (srcCell != null) {
//                        DoubleCorneredEdge edge = new DoubleCorneredEdge(tgtCell, srcCell, Orientation.VERTICAL);
//                        model.addEdge(edge);
//                        model.addEdge(tgtCell, srcCell);
                        model.addEdge(new DirectedEdgeFromToLabel(tgtCell, srcCell, src.getInputTag(), tag));
                    }
                });
            }
        });
        outputGraph.endUpdate();
        return outputGraph;
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

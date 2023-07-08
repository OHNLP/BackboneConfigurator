package org.ohnlp.backbone.configurator.gui.utils;

import com.fxgraph.graph.Graph;
import com.fxgraph.graph.Model;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.components.HasInputs;
import org.ohnlp.backbone.api.components.HasOutputs;
import org.ohnlp.backbone.configurator.gui.components.graphs.ComponentCell;
import org.ohnlp.backbone.configurator.gui.components.graphs.DirectedEdgeFromToLabel;
import org.ohnlp.backbone.configurator.structs.pipeline.EditablePipeline;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAGUtils {
    public static Graph generateGraphForPipeline(EditablePipeline pipeline) {
        List<List<PipelineComponentDeclaration>> pipelineComponents = pipeline.getPipelineAsSteps();
        Graph outputGraph = new Graph();
        outputGraph.getUseNodeGestures().setValue(false);
        Model model = outputGraph.getModel();
        Map<String, PipelineComponentDeclaration> componentsByID = new HashMap<>();
        Map<String, ComponentCell> cellsByID = new HashMap<>();
        // Draw
        pipelineComponents.forEach(step -> {
            step.forEach(component -> {
                BackbonePipelineComponent componentInstance = component.getComponentDef().getInstance(component, false);
                ComponentCell cell = new ComponentCell(
                        component.getComponentID(),
                        component.getComponentDef().getName(),
                        component.getComponentDef().getDesc(),
                        componentInstance instanceof HasInputs ? ((HasInputs)componentInstance).getInputTags() : Collections.emptyList(),
                        componentInstance instanceof HasOutputs ? ((HasOutputs)componentInstance).getOutputTags() : Collections.emptyList(),
                        componentInstance,
                        component);
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
                        model.addEdge(new DirectedEdgeFromToLabel(tgtCell, srcCell, src.getInputTag(), tag));
                    }
                });
            }
        });
        outputGraph.endUpdate();
        return outputGraph;
    }
}

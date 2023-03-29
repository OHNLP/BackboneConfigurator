package org.ohnlp.backbone.configurator.structs.pipeline;

import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;

import java.util.List;

public class PipelineDAG {



    private static class PipelineDAGStep {
        private List<BackbonePipelineComponentConfiguration> components;
    }
}

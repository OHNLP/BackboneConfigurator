package org.ohnlp.backbone.configurator.structs.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;

import java.util.ArrayList;

public class PipelineConfigUtils {
    public static PipelineComponentDeclaration fromJSONConfig(EditablePipeline parent, BackbonePipelineComponentConfiguration config) {
        ModulePipelineComponentDeclaration module = ModuleRegistry.getComponentByClass(config.getClazz().getName());
        if (module == null) {
            // Declared module not found, discard TODO maybe log this somewhere?
            return null;
        }
        PipelineComponentDeclaration ret = new PipelineComponentDeclaration(parent);
        ret.setComponentID(config.getComponentID());
        ret.setComponentDef(module);
        ret.setConfig(new ArrayList<>());
        module.getConfigFields().forEach(f -> ret.getConfig().add(f.clone()));
        // Now inject configuration settings
        JsonNode componentConfig = config.getConfig();
        ret.getConfig().forEach(f -> {
            String[] path = f.getPath().split("\\.");
            JsonNode curr = componentConfig;
            for (String childPath : path) {
                if (curr.hasNonNull(childPath)) {
                    curr = curr.get(childPath);
                } else {
                    curr = null;
                    break;
                }
            }
            if (curr != null) {
                f.getImpl().injectValueFromJSON(curr);
            }
        });
        ret.setInputs(config.getInputs());
        return ret;
    }
}

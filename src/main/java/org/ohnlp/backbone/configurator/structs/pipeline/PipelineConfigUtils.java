package org.ohnlp.backbone.configurator.structs.pipeline;

import com.fasterxml.jackson.databind.JsonNode;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.config.xlang.JavaBackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.config.xlang.PythonBackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.configurator.ModuleRegistry;
import org.ohnlp.backbone.configurator.structs.modules.ModulePipelineComponentDeclaration;
import org.ohnlp.backbone.configurator.structs.modules.PythonModulePipelineComponentDeclaration;

import java.util.ArrayList;

public class PipelineConfigUtils {
    public static PipelineComponentDeclaration fromJSONConfig(EditablePipeline parent, BackbonePipelineComponentConfiguration config) {
        ModulePipelineComponentDeclaration module = null;
        if (config instanceof JavaBackbonePipelineComponentConfiguration) {
            module = ModuleRegistry.getComponentByClass(((JavaBackbonePipelineComponentConfiguration)config).getClazz().getName());
        } else if (config instanceof PythonBackbonePipelineComponentConfiguration) {
            String key = ((PythonBackbonePipelineComponentConfiguration) config).getBundleName() + ":" + ((PythonBackbonePipelineComponentConfiguration) config).getEntryPoint() + ":" + ((PythonBackbonePipelineComponentConfiguration) config).getEntryClass();
            module = ModuleRegistry.getComponentByClass(key);
        }
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

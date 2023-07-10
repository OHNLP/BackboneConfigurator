package org.ohnlp.backbone.configurator.structs.modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohnlp.backbone.api.components.xlang.python.PythonBackbonePipelineComponent;
import org.ohnlp.backbone.api.components.xlang.python.PythonProxyTransformComponent;
import org.ohnlp.backbone.api.exceptions.ComponentInitializationException;
import org.ohnlp.backbone.configurator.structs.modules.serde.PythonModulePipelineComponentDeclarationDeserializer;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.lang.reflect.Field;

@JsonDeserialize(using = PythonModulePipelineComponentDeclarationDeserializer.class)
public class PythonModulePipelineComponentDeclaration extends ModulePipelineComponentDeclaration {
    private String bundle_identifier;
    private String entry_point;
    private String class_name;


    public String getBundle_identifier() {
        return bundle_identifier;
    }

    public void setBundle_identifier(String bundle_identifier) {
        this.bundle_identifier = bundle_identifier;
    }

    public String getEntry_point() {
        return entry_point;
    }

    public void setEntry_point(String entry_point) {
        this.entry_point = entry_point;
    }

    public String getClass_name() {
        return class_name;
    }

    public void setClass_name(String class_name) {
        this.class_name = class_name;
    }

    @Override
    public PythonProxyTransformComponent getInstance(PipelineComponentDeclaration callingComponentDec, boolean loadConfig) {
        if (callingComponentDec.getInstance() != null) {
            if (loadConfig) { // Reinit config
                PythonProxyTransformComponent pyComponent = ((PythonProxyTransformComponent) callingComponentDec.getInstance());
                JsonNode conf = callingComponentDec.toBackboneConfigFormat().getConfig();
                pyComponent.injectConfig(conf);
                try {
                    Field f = PythonProxyTransformComponent.class.getDeclaredField("proxiedComponent");
                    f.setAccessible(true);
                    PythonBackbonePipelineComponent cmp = (PythonBackbonePipelineComponent) f.get(pyComponent);
                    cmp.init(new ObjectMapper().writeValueAsString(conf));
                } catch (NoSuchFieldException | IllegalAccessException | JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
            return (PythonProxyTransformComponent) callingComponentDec.getInstance();
        }
        PythonProxyTransformComponent ret = new PythonProxyTransformComponent(getBundle_identifier(), getEntry_point(), getClass_name());

        if (loadConfig) {
            ret.injectConfig(callingComponentDec.toBackboneConfigFormat().getConfig());
        }
        try {
            Runtime.getRuntime().addShutdownHook(new Thread(ret::teardown)); // Make sure bridge gets shut down on JVM close
            ret.init();
        } catch (ComponentInitializationException e) {
            throw new RuntimeException(e);
        }
        callingComponentDec.setInstance(ret);
        return ret;
    }
}

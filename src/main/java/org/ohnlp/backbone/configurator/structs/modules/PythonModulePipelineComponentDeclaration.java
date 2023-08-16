package org.ohnlp.backbone.configurator.structs.modules;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.ohnlp.backbone.api.components.xlang.python.PythonBackbonePipelineComponent;
import org.ohnlp.backbone.api.components.xlang.python.PythonProxyTransformComponent;
import org.ohnlp.backbone.api.exceptions.ComponentInitializationException;
import org.ohnlp.backbone.configurator.WorkerService;
import org.ohnlp.backbone.configurator.structs.modules.serde.PythonModulePipelineComponentDeclarationDeserializer;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.io.File;
import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

@JsonDeserialize(using = PythonModulePipelineComponentDeclarationDeserializer.class)
public class PythonModulePipelineComponentDeclaration extends ModulePipelineComponentDeclaration {
    private String bundle_identifier;
    private String entry_point;
    private String class_name;

    private final AtomicReference<CompletableFuture<PythonProxyTransformComponent>> initFuture = new AtomicReference<>();



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
    public CompletableFuture<PythonProxyTransformComponent> getInstance(PipelineComponentDeclaration callingComponentDec, boolean loadConfig) {
        if (callingComponentDec.getInstance() != null) { // Already initialized
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
            return CompletableFuture.completedFuture((PythonProxyTransformComponent) callingComponentDec.getInstance());
        } else {
            // Initialize an instance
            PythonProxyTransformComponent ret = new PythonProxyTransformComponent(new File("configurator_python_envs"), getBundle_identifier(), getEntry_point(), getClass_name());

            if (loadConfig) {
                ret.injectConfig(callingComponentDec.toBackboneConfigFormat().getConfig());
            }
            String taskName = "Initializing Component@"  + hashCode() + ": " + getName();
            synchronized (initFuture) {
                if (initFuture.get() == null) {
                    initFuture.set(WorkerService.schedule(taskName, () -> {
                        try {
                            Runtime.getRuntime().addShutdownHook(new Thread(ret::teardown)); // Make sure bridge gets shut down on JVM close
                            ret.init();
                            callingComponentDec.setInstance(ret);
                            return ret;
                        } catch (ComponentInitializationException e) {
                            throw new RuntimeException("Failed to create new component instance for " + getEntry_point() + ":" + getClass_name(), e);
                        }
                    }, false)
                    );
                }
                return initFuture.get();
            }
        }

    }
}

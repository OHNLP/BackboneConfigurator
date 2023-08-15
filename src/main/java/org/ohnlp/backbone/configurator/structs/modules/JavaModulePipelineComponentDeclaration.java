package org.ohnlp.backbone.configurator.structs.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.exceptions.ComponentInitializationException;
import org.ohnlp.backbone.api.pipeline.PipelineBuilder;
import org.ohnlp.backbone.configurator.WorkerService;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class JavaModulePipelineComponentDeclaration extends ModulePipelineComponentDeclaration {
    private Class<? extends BackbonePipelineComponent<?,?>> clazz;
    private AtomicReference<CompletableFuture<BackbonePipelineComponent<?, ?>>> initFuture = new AtomicReference<>();

    public JavaModulePipelineComponentDeclaration(Class<? extends BackbonePipelineComponent<?,?>> clazz) {
        this.clazz = clazz;
    }


    public Class<? extends BackbonePipelineComponent<?,?>> getClazz() {
        return clazz;
    }

    public void setClazz(Class<? extends BackbonePipelineComponent<?,?>> clazz) {
        this.clazz = clazz;
    }

    @Override
    public CompletableFuture<BackbonePipelineComponent<?, ?>> getInstance(PipelineComponentDeclaration callingComponentDec, boolean loadConfig) {
        try {
            Constructor<? extends BackbonePipelineComponent<?, ?>> ctor =
                    getClazz().getDeclaredConstructor();
            BackbonePipelineComponent<?, ?> ret = ctor.newInstance();
            if (loadConfig) {
                Method m = PipelineBuilder.class.getDeclaredMethod("injectInstanceWithConfigurationProperties", Class.class, BackbonePipelineComponent.class, JsonNode.class);
                m.setAccessible(true);
                try {
                    m.invoke(null, ret.getClass(), ret, callingComponentDec.toBackboneConfigFormat().getConfig());
                } catch (InvocationTargetException ignored) {
                }
                String taskName = "Initializing Component@"  + hashCode() + ": " + getName();
                synchronized (initFuture) {
                    if (initFuture.get() == null) {
                        initFuture.set(WorkerService.schedule(taskName, () -> {
                            try {
                                ret.init();
                                return ret;
                            } catch (ComponentInitializationException e) {
                                throw new RuntimeException("Failed to create new component instance for " + getClazz().getName(), e);
                            }
                        }, false));
                    }
                    return initFuture.get();
                }
            } else {
                return CompletableFuture.completedFuture(ret);
            }
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create new component instance for " + getClazz().getName(), t);
        }
    }
}

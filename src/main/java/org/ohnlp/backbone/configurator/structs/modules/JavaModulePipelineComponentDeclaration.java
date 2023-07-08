package org.ohnlp.backbone.configurator.structs.modules;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.ohnlp.backbone.api.BackbonePipelineComponent;
import org.ohnlp.backbone.api.config.BackbonePipelineComponentConfiguration;
import org.ohnlp.backbone.api.exceptions.ComponentInitializationException;
import org.ohnlp.backbone.api.pipeline.PipelineBuilder;
import org.ohnlp.backbone.configurator.structs.pipeline.PipelineComponentDeclaration;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class JavaModulePipelineComponentDeclaration extends ModulePipelineComponentDeclaration {
    private Class<? extends BackbonePipelineComponent<?,?>> clazz;

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
    public BackbonePipelineComponent<?, ?> getInstance(PipelineComponentDeclaration callingComponentDec, boolean loadConfig) {
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
                try {
                    ExecutorService executor = Executors.newSingleThreadExecutor();
                    Future<ComponentInitializationException> future = executor.submit(() -> {
                        try {
                            ret.init();
                            return null;
                        } catch (ComponentInitializationException e) {
                            return e;
                        }
                    });
                    ComponentInitializationException initException;
                    try {
                        initException = future.get(5000, TimeUnit.MILLISECONDS);
                    } catch (Throwable ignored) {
                        // TODO
                    }
                    executor.shutdownNow();
                } catch (Throwable t) {
                    t.printStackTrace();
                }
            }
            return ret;
        } catch (Throwable t) {
            throw new RuntimeException("Failed to create new component instance for " + getClazz().getName(), t);
        }
    }
}

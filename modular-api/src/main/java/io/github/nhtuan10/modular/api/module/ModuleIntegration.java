package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;

public interface ModuleIntegration {

    static ModuleIntegration getInstance() {
        try {
            Class<ModuleIntegration> implementationClass = Utils.getImplementationClass(ModuleIntegration.class, ModuleIntegration.class.getClassLoader());
            Method method = implementationClass.getDeclaredMethod("getInstance");
            method.setAccessible(true);
            return (ModuleIntegration) method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new ModularRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }

    <T> BlockingQueue<T> getBlockingQueue(String name, Class<T> type);

    <T> Queue<T> getQueue(String name, Class<T> type);

    <T> Queue<T> getQueue(String name, Class<T> type, Class<? extends Queue> queueClass);
}

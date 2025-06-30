package io.github.nhtuan10.modular.context;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceLookUpRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.module.ModularClassLoader;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@AllArgsConstructor
public class ModuleContextImpl implements ModuleContext {
    private Object moduleLoader;

    public <S> List<S> getModularServicesFromSpring(Class<?> clazz) {
        try {
            return (List<S>) moduleLoader.getClass().getMethod("getModularServices", Class.class, boolean.class).invoke(moduleLoader, clazz, true);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new ServiceLookUpRuntimeException("Failed to get modular services from Spring for class " + clazz.getName(), e);
        }
    }

    public <S> List<S> getModularServices(Class<?> clazz) {
        try {
            return (List<S>) moduleLoader.getClass().getMethod("getModularServices", Class.class, boolean.class).invoke(moduleLoader, clazz, false);
        } catch (InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
            throw new ServiceLookUpRuntimeException("Failed to get modular services for class " + clazz.getName(), e);
        }
    }

    @Override
    public void notifyModuleReady() {
        ClassLoader classLoader = this.getClass().getClassLoader();
        if (ModularClassLoader.class.getName().equals(classLoader.getClass().getName())) {
            try {
                String moduleName = classLoader.getClass().getMethod("getModuleName").invoke(classLoader).toString();
                moduleLoader.getClass().getMethod("notifyModuleReady", String.class).invoke(moduleLoader, moduleName);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new ModuleLoadRuntimeException("Exception during ModularContext#notifyModuleReady", e);
            }
        } else {
            throw new ModuleLoadRuntimeException("Illegal invocation of ModularContext#notifyModuleReady");
        }
    }
}

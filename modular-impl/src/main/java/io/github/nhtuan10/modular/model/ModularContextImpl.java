package io.github.nhtuan10.modular.model;

import io.github.nhtuan10.modular.api.model.ModularContext;
import io.github.nhtuan10.modular.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.ModuleLoadException;
import lombok.AllArgsConstructor;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

@AllArgsConstructor
public class ModularContextImpl implements ModularContext {
    private Object moduleLoader;

    public <S> List<S> getModularServicesFromSpring(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (List<S>) moduleLoader.getClass().getDeclaredMethod("getModularServices", Class.class, boolean.class).invoke(moduleLoader, clazz, true);
    }

    public <S> List<S> getModularServices(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        return (List<S>) moduleLoader.getClass().getDeclaredMethod("getModularServices", Class.class, boolean.class).invoke(moduleLoader, clazz, false);
    }

    @Override
    public void notifyModuleReady() {
        ClassLoader classLoader = this.getClass().getClassLoader();
//        var mcl = ModularClassLoader.class.getClassLoader();
        if (ModularClassLoader.class.getName().equals(classLoader.getClass().getName())) {
//        if (classLoader instanceof ModularClassLoader modularClassLoader) {
            try {
                String moduleName = classLoader.getClass().getDeclaredMethod("getModuleName").invoke(classLoader).toString();
                moduleLoader.getClass().getDeclaredMethod("notifyModuleReady", String.class).invoke(moduleLoader, moduleName);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new ModuleLoadException("Exception during ModularContext#notifyModuleReady", e);
            }
        } else {
            throw new ModuleLoadException("Illegal invocation of ModularContext#notifyModuleReady");
        }
    }
}

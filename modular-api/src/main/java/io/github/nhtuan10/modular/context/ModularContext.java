package io.github.nhtuan10.modular.context;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.api.module.Utils;

import java.lang.reflect.InvocationTargetException;

public final class ModularContext {
    //    private static final ModuleContext INSTANCE ;
    private static final Object INSTANCE;
    private static final ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
    private static final Class<ModuleContext> implementationClass;

    static {
        try {
            implementationClass = Utils.getImplementationClass(ModuleContext.class, systemClassLoader);
//            INSTANCE = implementationClass.getConstructor(ModuleLoader.class).newInstance(ModuleLoader.getInstance());
            Class<?> moduleLoaderClass = Class.forName(ModuleLoader.class.getName(), true, systemClassLoader);
            Object moduleLoaderInstance = moduleLoaderClass.getMethod("getInstance").invoke(null);
            INSTANCE = implementationClass.getConstructor(moduleLoaderClass).newInstance(moduleLoaderInstance);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException |
                 ClassNotFoundException e) {
            throw new ModularRuntimeException("Couldn't create any ModuleContext implementation instance", e);
        }
    }

    public static void notifyModuleReady() {
        String moduleName = getCurrentModuleName();
        notifyModuleReady(moduleName);
    }

    public static String getCurrentModuleName() {
        // TODO: check if current class loader is modular class loader or not in a better way
        if (ModularContext.class.getClassLoader() instanceof ModularClassLoader || "io.github.nhtuan10.modular.impl.classloader.DefaultModularClassLoader".equals(ModularContext.class.getClassLoader().getParent().getClass().getName())) {
            try {
                return (String) implementationClass.getDeclaredMethod("getCurrentModuleName").invoke(INSTANCE);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ModularRuntimeException("Can't invoke getCurrentModuleName", e);
            }
//            INSTANCE.notifyModuleReady();
        } else {
            return null;
        }
//        return INSTANCE.getCurrentModuleName();
    }

    public static void notifyModuleReady(String moduleName) {
        // TODO: check if current class loader is modular class loader or not in a better way
        if (ModularContext.class.getClassLoader() instanceof ModularClassLoader || "io.github.nhtuan10.modular.impl.classloader.DefaultModularClassLoader".equals(ModularContext.class.getClassLoader().getParent().getClass().getName())) {
            try {
                implementationClass.getDeclaredMethod("notifyModuleReady", String.class).invoke(INSTANCE, moduleName);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                throw new ModularRuntimeException("Can't invoke getCurrentModuleName", e);
            }
//            INSTANCE.notifyModuleReady();
        }
//        else {
//            return null;
//        }
//        INSTANCE.notifyModuleReady(moduleName);
    }
}

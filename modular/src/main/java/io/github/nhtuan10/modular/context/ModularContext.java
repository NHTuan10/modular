package io.github.nhtuan10.modular.context;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.api.module.Utils;

import java.lang.reflect.InvocationTargetException;

public final class ModularContext {
    private static final ModuleContext INSTANCE ;
    static {
        try {
            Class<ModuleContext> implementationClass = Utils.getImplementationClass(ModuleContext.class, ModularContext.class.getClassLoader());
//            INSTANCE = implementationClass.getConstructor(ModuleLoader.class).newInstance(ModuleLoader.getInstance());
            Class<?> moduleLoaderClass = ClassLoader.getSystemClassLoader().loadClass(ModuleLoader.class.getName());
            Object moduleLoaderInstance = moduleLoaderClass.getMethod("getInstance").invoke(null);
            INSTANCE = implementationClass.getConstructor(moduleLoaderClass).newInstance(moduleLoaderInstance);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException |
                 ClassNotFoundException e) {
            throw new ModularRuntimeException("Couldn't create any ModuleContext implementation instance", e);
        }
    }

    public static void notifyModuleReady() {
        if (ModularContext.class.getClassLoader() instanceof ModularClassLoader) {
            INSTANCE.notifyModuleReady();
        }
    }

    public static String getCurrentModuleName() {
        return INSTANCE.getCurrentModuleName();
    }

    public static void notifyModuleReady(String moduleName) {
        INSTANCE.notifyModuleReady(moduleName);
    }
}

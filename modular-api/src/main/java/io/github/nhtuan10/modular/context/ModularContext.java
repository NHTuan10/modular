package io.github.nhtuan10.modular.context;

import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.api.module.Utils;

import java.lang.reflect.InvocationTargetException;

public final class ModularContext {
    private static final ModuleContext INSTANCE ;
    static {
        try {
            Class<ModuleContext> implementationClass = Utils.getImplementationClass(ModuleContext.class, ModularContext.class);
            INSTANCE = implementationClass.getConstructor(ModuleLoader.class).newInstance(ModuleLoader.getInstance());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new ModularRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }

    public static void notifyModuleReady(){
        INSTANCE.notifyModuleReady();
    }

    public static String getCurrentModuleName() {
        return INSTANCE.getCurrentModuleName();
    }
}

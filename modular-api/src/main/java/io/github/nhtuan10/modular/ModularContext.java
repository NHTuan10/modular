package io.github.nhtuan10.modular;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.api.module.Utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class ModularContext {
    private static final ModuleContext INSTANCE ;
    static {
        try {
            Class<ModuleContext> implementationClass = Utils.getImplementationClass(ModuleContext.class, ModularContext.class);
            INSTANCE = implementationClass.getConstructor(Object.class).newInstance(ModuleLoader.getInstance());
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | InstantiationException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }
    public static void notifyModuleReady(){
        INSTANCE.notifyModuleReady();
    }
}

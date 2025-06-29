package io.github.nhtuan10.modular.api;

import io.github.nhtuan10.modular.api.module.ModuleLoader;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface Modular {
    static ModuleLoader.ModuleDetail startModuleSync(String moduleName, List<String> locationUris, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleSync(moduleName, locationUris, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleSyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startModuleAsync(String moduleName, List<String> locationUris, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleAsync(moduleName, locationUris, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startModuleAsyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleSyncWithMainClassLoop(moduleName, locationUris, mainClass, packagesToScan);
    }

    static ModuleLoader.ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleSyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan){
        return ModuleLoader.getInstance().startSpringModuleAsyncWithMainClassLoop(moduleName, locationUris, mainClass, packagesToScan);
    }

    static CompletableFuture<ModuleLoader.ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan) {
        return ModuleLoader.getInstance().startSpringModuleAsyncWithMainClass(moduleName, locationUris, mainClass, packagesToScan);
    }

    static boolean unloadModule(String moduleName) {
        return ModuleLoader.getInstance().unloadModule(moduleName);
    }

    static boolean isManaged(Object object){
        return ModuleLoader.isManaged(object);
    }

    static boolean isManaged(Class<?> clazz){
        return ModuleLoader.isManaged(clazz);
    }

    static void notifyModuleReady(){
        ModuleLoader.getContext().notifyModuleReady();
    }

    static <S> List<S> getModularServicesFromSpring(Class<?> clazz) {
        return ModuleLoader.getContext().<S>getModularServicesFromSpring(clazz);
    }

    static <S> List<S> getModularServices(Class<?> clazz) {
        return ModuleLoader.getContext().<S>getModularServices(clazz);
    }
}

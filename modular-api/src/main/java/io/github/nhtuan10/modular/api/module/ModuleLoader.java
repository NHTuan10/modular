package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import lombok.*;

import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public interface ModuleLoader {
    static ModuleLoader getInstance() {
        try {
            Class<?> implementationClass = getImplementationClass();
            return (ModuleLoader) implementationClass.getDeclaredMethod("getInstance").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }

    static Class<?> getImplementationClass() {
        ServiceLoader<ModuleLoader> loader = ServiceLoader.load(ModuleLoader.class);
        if (loader.findFirst().isPresent()) {
            ModuleLoader moduleLoader = loader.findFirst().get();
            return moduleLoader.getClass();
        } else {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation class");
        }
    }

    static ModuleContext getContext() {
        ModuleLoader moduleLoader = getInstance();
        try {
            return (ModuleContext) moduleLoader.getClass().getDeclaredMethod("getContext").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            throw new ModuleLoadRuntimeException("Error getting ModuleContext", e);
        }

    }

    ModuleDetail startModuleSync(String moduleName, String locationUri, String packageToScan);

    ModuleDetail startModuleSyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan);

    CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, String locationUri, String packageToScan);

    CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan);

    ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, String locationUri, String mainClass, String packageToScan);

    ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan);

    CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, String locationUri, String mainClass, String packageToScan);

    CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan);

    boolean unloadModule(String moduleName);

    enum LoadStatus {
        LOADING,
        LOADED,
        FAILED,
    }

    @AllArgsConstructor
    class ModuleDetail {
        @Getter
        private final String moduleName;
        @Getter
        @Setter
        private ModuleLoader.LoadStatus loadStatus;
        @Getter
        @Setter
        private ClassLoader classLoader;
        @Getter
        private CountDownLatch readyLatch;
        @Getter
        private CountDownLatch awaitMainClassLatch;
    }

    @Builder
    class ModuleLoaderConfiguration {
//        @Builder.Default
//        @Getter
//        private final int threadPoolSize = 10;
    }
}

package io.github.nhtuan10.modular.classloader;

import io.github.nhtuan10.modular.api.model.ModularContext;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public interface ModuleLoader {
    static ModuleLoader getInstance() {
        return ModuleLoaderImpl.getInstance(ModuleLoaderImpl.ModuleLoaderConfiguration.builder().build());
    }

    static ModularContext getContext() {
        return ModuleLoaderImpl.getContext();
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

    @NoArgsConstructor
    @AllArgsConstructor
    class ModuleDetail {
        @Getter
        String moduleName;
        @Getter
        ModuleLoaderImpl.LoadStatus loadStatus;
        @Getter
        ModularClassLoader modularClassLoader;
        CountDownLatch readyLatch;
        CountDownLatch awaitMainClassLatch;
    }
}

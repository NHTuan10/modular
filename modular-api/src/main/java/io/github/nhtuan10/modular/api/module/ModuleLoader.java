package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import lombok.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public interface ModuleLoader {

    String MODULAR_IMPL_CLASS_CONFIG_FILE = "META-INF/services/io.github.nhtuan10.modular.api.module.ModuleLoader";
    Pattern classLoaderNamePattern = Pattern.compile("^io.github.nhtuan10.modular.module.ModularClassLoader\\[.+\\]$");

    static ModuleLoader getInstance() {
        try {
            Class<?> implementationClass = getImplementationClass();
            return (ModuleLoader) implementationClass.getDeclaredMethod("getInstance").invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }

    static Class<?> getImplementationClass() {
//        ServiceLoader<ModuleLoader> loader = ServiceLoader.load(ModuleLoader.class);
//        if (loader.findFirst().isPresent()) {
//            ModuleLoader moduleLoader = loader.findFirst().get();
//            return moduleLoader.getClass();
//        }
//        else {
//            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation class");
//        }
        try (InputStream is = ModuleLoader.class.getClassLoader().getResourceAsStream(MODULAR_IMPL_CLASS_CONFIG_FILE)){
            if (is != null) {
                String clazz = new String(is.readAllBytes());
                return Class.forName(clazz);
            }
            else {
                throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation class");
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation class", e);
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

    ModuleDetail startModuleSync(String moduleName, List<String> locationUris, List<String> packagesToScan);

    ModuleDetail startModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, List<String> locationUris, List<String> packagesToScan);

    CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan);

    boolean unloadModule(String moduleName);

    static boolean isManaged(Object object){
        return isManaged(object.getClass());
    }

    static boolean isManaged(Class<?> clazz){
        String classLoaderName = clazz.getClassLoader().getName();
        return classLoaderName != null && classLoaderNamePattern.matcher(classLoaderName).matches();
    }

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

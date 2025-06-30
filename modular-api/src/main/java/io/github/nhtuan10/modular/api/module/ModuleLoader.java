package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;

public interface ModuleLoader {

    Pattern classLoaderNamePattern = Pattern.compile("^io.github.nhtuan10.modular.module.ModularClassLoader\\[.+\\]$");

    static ModuleLoader getInstance() {
        try {
            Class<ModuleLoader> implementationClass = Utils.getImplementationClass(ModuleLoader.class, ModuleLoader.class);
            Method method = implementationClass.getDeclaredMethod("getInstance");
            method.setAccessible(true);
            return (ModuleLoader) method.invoke(null);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
        }
    }

    static ModuleContext getContext() {
        try {
            Class<?> moduleLoaderImplClass = getInstance().getClass();
            Method m = moduleLoaderImplClass.getDeclaredMethod("getContext");
            return (ModuleContext) m.invoke(null);
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

    static boolean isManaged(Object object) {
        return isManaged(object.getClass());
    }

    static boolean isManaged(Class<?> clazz) {
        String classLoaderName = clazz.getClassLoader().getName();
        return classLoaderName != null && classLoaderNamePattern.matcher(classLoaderName).matches();
    }

    enum LoadStatus {
        NEW,
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
        public static enum SerializeType {
            JACKSON_SMILE,
            JAVA,
            KRYO
        }

        @Builder.Default
        @Getter
        private SerializeType serializeType = SerializeType.KRYO;

        public static final ModuleLoaderConfiguration DEFAULT = ModuleLoaderConfiguration.builder().build();
    }
}

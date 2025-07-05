package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
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
            throw new ModularRuntimeException("Couldn't find any ModuleLoader implementation instance", e);
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

    <I> List<I> getModularServices(Class<I> clazz);

    <I> List<I> getModularServices(Class<I> clazz, boolean copyTransClassLoaderObjects);

    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz);

    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, boolean copyTransClassLoaderObjects);

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
        public enum SerializeType {
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

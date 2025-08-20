package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

public interface ModuleLoader {
    public static String PROXY_TARGET_FIELD_NAME = "target";

//    Pattern classLoaderNamePattern = Pattern.compile("^io.github.nhtuan10.modular.impl.classloader.ModularClassLoader\\[.+\\]$");

    static ModuleLoader getInstance() {
        try {
            Class<ModuleLoader> implementationClass = Utils.getImplementationClass(ModuleLoader.class, ModuleLoader.class.getClassLoader());
            Method method = implementationClass.getDeclaredMethod("getInstance");
            method.setAccessible(true);
            Object target = method.invoke(null);
            return Utils.createProxyObject(ModuleLoader.class, target);
        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException | NoSuchFieldException |
                 ClassNotFoundException | InstantiationException e) {
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

    ModuleDetail startModuleSync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration);

    CompletableFuture<ModuleDetail> startModuleASync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration);

//    <I> List<I> getModularServices(Class<I> clazz);
//
//    <I> List<I> getModularServices(Class<I> clazz, String moduleName);
//
//    <I> List<I> getModularServices(Class<I> clazz, boolean copyTransClassLoaderObjects);
//
//    <I> List<I> getModularServices(String name, Class<I> clazz, ExternalContainer externalContainer);
//
//    <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer);

    <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer, boolean copyTransClassLoaderObjects);
//
//    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, String moduleName);
//
//    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz);
//
//    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, boolean copyTransClassLoaderObjects);

    <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, String moduleName, boolean copyTransClassLoaderObjects);

    boolean unloadModule(String moduleName);

    //    <I> List<I> getModularServices(String name, Class<I> clazz,ExternalContainer externalContainer, boolean copyTransClassLoaderObjects);

    //    <I> List<I> getModularServices(Class<I> clazz, String moduleName, boolean copyTransClassLoaderObjects);


    static boolean isManaged(Object object) {
        return isManaged(object.getClass());
    }

    static boolean isManaged(Class<?> clazz) {
//        String classLoaderName = clazz.getClassLoader().getName();
//        return classLoaderName != null && classLoaderNamePattern.matcher(classLoaderName).matches();
        // TODO: check if current class loader is modular class loader or not in a better way
        ClassLoader classLoader = clazz.getClassLoader();
        return (classLoader instanceof ModularClassLoader || "io.github.nhtuan10.modular.impl.classloader.DefaultModularClassLoader".equals(classLoader.getParent().getClass().getName()));
    }

    String getCurrentModuleName();

    void notifyModuleReady(String moduleName);

    enum LoadStatus implements Serializable {
        NEW,
        LOADING,
        LOADED,
        FAILED,
    }

    @AllArgsConstructor
    class ModuleDetail implements Serializable {
        @Getter
        private final String moduleName;
        @Getter
        @Setter
        private ModuleLoader.LoadStatus loadStatus;
        @Getter
        @Setter
        private transient ModularClassLoader classLoader;
        @Getter
        private transient CountDownLatch readyLatch;
        @Getter
        private transient CountDownLatch awaitMainClassLatch;
    }

    @Builder
    class ModuleLoaderConfiguration {
        public enum SerializeType {
            //            JACKSON_SMILE,
            JAVA,
            KRYO
        }

        @Builder.Default
        @Getter
        private SerializeType serializeType = SerializeType.KRYO;

        public static final ModuleLoaderConfiguration DEFAULT = ModuleLoaderConfiguration.builder().build();
    }
}

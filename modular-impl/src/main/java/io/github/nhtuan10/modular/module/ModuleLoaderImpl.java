package io.github.nhtuan10.modular.module;

import io.github.nhtuan10.modular.annotation.ModularAnnotationProcessor;
import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.model.ArtifactLocationType;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.classloader.MavenArtifactsResolver;
import io.github.nhtuan10.modular.model.ModularServiceHolder;
import io.github.nhtuan10.modular.proxy.ServiceInvocationInterceptor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

@Slf4j
public class ModuleLoaderImpl implements ModuleLoader {
    public static final String APPLICATION_CONTEXT_PROVIDER = "io.github.nhtuan10.modular.spring.ApplicationContextProvider";
    public static final String PROXY_TARGET_FIELD_NAME = "target";
    //    public static final String MODULAR_ANNOTATION_PKG = "io.github.nhtuan10.modular.annotation";
    //    CustomClassLoader classLoader;

    //    ModularAnnotationProcessor m;

    Map<Class<?>, Collection<ModularServiceHolder>> loadedModularServices = new ConcurrentHashMap<>(); //UNUSED
    Map<String, Collection<ModularServiceHolder>> loadedModularServices2 = new ConcurrentHashMap<>();
    //    Map<String, ModularClassLoader> modularClassLoaders = new ConcurrentHashMap<>();
    Map<Class<?>, List<Object>> loadedProxyObjects = new ConcurrentHashMap<>();
    Map<String, ModuleDetail> moduleDetailMap = new ConcurrentHashMap<>();

//    Executor executor;


    private volatile static ModuleLoader instance;
    private static final Object lock = new Object();

    public static ModuleLoader getInstance() {
        return ModuleLoaderImpl.getInstance(ModuleLoader.ModuleLoaderConfiguration.builder().build());
    }


    public static ModuleLoader getInstance(ModuleLoaderConfiguration configuration) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ModuleLoaderImpl();
//                    instance.executor = Executors.newFixedThreadPool(configuration.getThreadPoolSize());
                }
            }
        }
        return instance;
    }

    public ModuleLoaderImpl() {
    }

    public void loadModule(String name, String locationUri, boolean lazyInit) throws MalformedURLException {
        loadModule(name, locationUri, "*", lazyInit);
    }

    public void loadModule(String name, String locationUri, String packageToScan, boolean lazyInit) {
        // Load module
        URI uri = URI.create(locationUri);
        log.info("Loading module from " + uri);
        switch (ArtifactLocationType.valueOf(uri.getScheme().toUpperCase())) {
            case MVN:
                loadModuleFromMaven(name, uri, packageToScan, lazyInit);
                break;
            case FILE:
                loadModuleFromFile(name, uri, packageToScan, lazyInit);
                break;
            default:
                throw new IllegalArgumentException("Unsupported artifact location type: " + uri.getScheme());
        }
//        synchronized (classLoader) {


//        }
    }

    private void loadModuleFromMaven(String name, URI uri, String packageToScan, boolean lazyInit) {
        // Load module from Maven
        String mvnArtifact = uri.getHost() + uri.getPath().replace("/", ":");
//        log.info("Loading module from Maven: {}", mvnArtifact);
        List<URL> depUrls = new MavenArtifactsResolver<URL>().resolveMavenDeps(List.of(mvnArtifact), URL.class);
        loadModuleFromUrls(name, packageToScan, lazyInit, depUrls);
    }

    private void loadModuleFromUrls(String name, String packageToScan, boolean lazyInit, List<URL> depUrls) {
        ModularClassLoader classLoader = new ModularClassLoader(name, depUrls);
//        classLoader.setExcludedClassPackages(Set.of(MODULAR_ANNOTATION_PKG));
        ModuleDetail moduleDetail = moduleDetailMap.get(name);
        moduleDetail.setClassLoader(classLoader);

        ModularAnnotationProcessor m = new ModularAnnotationProcessor(classLoader);
        try {
            m.annotationProcess(packageToScan, lazyInit);
//            m.configurationAnnotationProcessor(packageToScan);
            addModularServices(m.getModularServices());
        }
//        catch (ProxyCreationException e) {
//            log.error("Fail to create proxy", e);
//            throw new RuntimeException(e);
//        }
        catch (Exception e) {
//            log.error("Fail during annotation processing", e);
            throw new RuntimeException(e);
        }
    }

    private void addModularServices(Map<Class<?>, Collection<ModularServiceHolder>> container) {
        // TODO: revise the implementation for thread-safety
        synchronized (lock) {
            container.forEach((key, value) -> {
                if (!loadedModularServices.containsKey(key)) {
                    loadedModularServices.put(key, Collections.synchronizedSet(new HashSet<>()));
                }
                loadedModularServices.get(key).addAll(value);
                if (!loadedModularServices2.containsKey(key.getName())) {
                    loadedModularServices2.put(key.getName(), Collections.synchronizedSet(new HashSet<>()));
                }
                loadedModularServices2.get(key.getName()).addAll(value);
            });
        }
    }

    private void loadModuleFromFile(String name, URI uri, String packageToScan, boolean lazyInit) {
        // Load module from file
        log.debug("Loading module {} from file {} with package", name, uri, packageToScan);
        try {
            loadModuleFromUrls(name, packageToScan, lazyInit, List.of(uri.toURL()));
        } catch (MalformedURLException e) {
            throw new ModuleLoadRuntimeException("Error loading module %s from file %s with package %s".formatted(name, uri, packageToScan), e);
        }
    }

    public static ModuleContext getContext() {
        Object moduleLoader = null;
        try {
            moduleLoader = Class.forName(ModuleLoaderImpl.class.getName(), true, ClassLoader.getSystemClassLoader()).getDeclaredMethod("getInstance").invoke(null);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                 ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return new ModuleContextImpl(moduleLoader);
    }

    public Collection<ModularServiceHolder> getModularServiceHolder(Class<?> key) {
        return loadedModularServices.get(key);
    }

    public Collection<ModularServiceHolder> getModularServiceHolder(String module, String key) throws ClassNotFoundException {
        return loadedModularServices.get(moduleDetailMap.get(module).getClassLoader().loadClass(key));
    }

    public Class<?> loadClass(String module, String name) throws ClassNotFoundException {
        return moduleDetailMap.get(module).getClassLoader().loadClass(name);
    }

    public ClassLoader getClassLoader(String module) {
        return moduleDetailMap.get(module).getClassLoader();
    }

//    public Object getModularServiceByExactClass(Class<?> key){
//        return loadedModularServices.get(key).stream().findFirst().map(ModularServiceHolder::getProxyObject).orElse(null);
//    }

    public <I> List<I> getModularServices(Class<?> apiClass, boolean fromSpringAppContext) {
        Collection<ModularServiceHolder> serviceHolders = loadedModularServices2.get(apiClass.getName());
        if (!loadedProxyObjects.containsKey(apiClass)) {
            List<I> proxyObjects = serviceHolders.stream()
//                .map(ModularServiceHolder::getServiceClass)
                    .map(serviceHolder -> {
                        try {
                            Object service;
                            if (fromSpringAppContext) {
                                Class serviceClass = serviceHolder.getServiceClass();
                                Class serviceAppContextProvide = Class.forName(APPLICATION_CONTEXT_PROVIDER, true, serviceClass.getClassLoader());
                                service = serviceAppContextProvide.getDeclaredMethod("getBean", Class.class).invoke(null, serviceClass);
                            } else {
                                service = serviceHolder.getInstance();
                            }
                            return this.<I>createProxyObject(apiClass, service);
                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                 NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
                            return null;
                        }
                    }).toList();
            loadedProxyObjects.put(apiClass, (List<Object>) proxyObjects);
            return proxyObjects;
        } else {
            return (List<I>) loadedProxyObjects.get(apiClass);
        }
    }

//    public <I> List<I> getModularServices(Class<?> apiClass) {
//
//        Collection<ModularServiceHolder> serviceHolders = loadedModularServices2.get(apiClass.getName());
//        if (!loadedProxyObjects.containsKey(apiClass)) {
//            List<I> proxyObjects = serviceHolders.stream().map(serviceHolder -> {
//                try {
//                    Object service = serviceHolder.getInstance();
//                    return this.<I>createProxyObject(apiClass, service);
//                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
//                         NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
//                    throw new RuntimeException(e);
//                }
//            }).toList();

    /// /            assertThat((String) dynamicType.newInstance().apply("Byte Buddy"), is("Hello from Byte Buddy"));
//            loadedProxyObjects.put(apiClass, (List<Object>) proxyObjects);
//            return proxyObjects;
//        } else {
//            return (List<I>) loadedProxyObjects.get(apiClass);
//        }
//    }
    private <I> I createProxyObject(Class<?> apiClass, Object service) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        ClassLoader apiClassLoader = apiClass.getClassLoader();
        Object svcInvocationInterceor = apiClassLoader.loadClass(ServiceInvocationInterceptor.class.getName())
                .getConstructor(Object.class).newInstance(service);
        Class<I> c = (Class<I>) new ByteBuddy()
                .<I>subclass(apiClass)
                .method(ElementMatchers.any())
                .intercept(MethodDelegation.to(svcInvocationInterceor))
                .defineField(PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
                .make()
                .load(apiClassLoader)
                .getLoaded();
        I proxy = c.getConstructor(new Class[]{}).newInstance();
        Field targetField = c.getDeclaredField(PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, service);
        return proxy;
    }

    private CompletableFuture<ModuleDetail> startModule(String moduleName, String locationUri, boolean lazyInit, String mainClass, String packageToScan, boolean awaitMainClass) {
        assert (moduleName != null && StringUtils.isNotBlank(moduleName)) : "Module name cannot be null or empty";
        CompletableFuture<ModuleDetail> moduleDetailCompletableFuture = new CompletableFuture<>();
        if (!moduleDetailMap.containsKey(moduleName)) {
            CountDownLatch await = new CountDownLatch(1);
            ModuleDetail moduleDetail = new ModuleDetail(moduleName, LoadStatus.LOADING, null, new CountDownLatch(1), await);
            moduleDetailMap.put(moduleName, moduleDetail);

            Thread t = new Thread(() -> {
                try {
                    loadModule(moduleName, locationUri, packageToScan, lazyInit);
                    Thread.currentThread().setContextClassLoader(getClassLoader(moduleName));
                    if (mainClass != null) {
                        try {
                            loadClass(moduleName, mainClass).getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[]{});
                            notifyModuleReady(moduleName);
                            moduleDetailCompletableFuture.complete(moduleDetail);
                            log.info("Finish loading module '{}'", moduleName);
                            if (awaitMainClass) {
                                Runtime.getRuntime().addShutdownHook(new Thread(await::countDown));
                                await.await();
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                                 ClassNotFoundException | InterruptedException e) {
                            throw new ModuleLoadRuntimeException("Error starting module", e);
                        }
                    } else {
                        notifyModuleReady(moduleName);
                        moduleDetailCompletableFuture.complete(moduleDetail);
                        log.info("Finish loading module '{}'", moduleName);
                    }
                } catch (Exception e) {
                    ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException("Failed to load module '" + moduleName, e);
                    moduleDetailCompletableFuture.completeExceptionally(exception);
                    throw e;
                }
            });
            t.start();
            return moduleDetailCompletableFuture;
        } else {
            ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException("Module '" + moduleName + "' is already loaded");
            moduleDetailCompletableFuture.completeExceptionally(exception);
            throw exception;
        }
    }

    @Override
    public ModuleDetail startModuleSync(String moduleName, String locationUri, String packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUri, false, null, packageToScan, false);
//        return cf.join();
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startModuleSyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUri, false, mainClass, packageToScan, false);
//        return cf.join();
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, String locationUri, String packageToScan) {
        return startModule(moduleName, locationUri, false, null, packageToScan, false);
    }

//    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, String locationUri) {
//        return startModuleAsync(moduleName, locationUri, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan) {
        return startModule(moduleName, locationUri, false, mainClass, packageToScan, false);
    }

    // Spring

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, String locationUri, String mainClass, String packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUri, true, mainClass, packageToScan, true);
        return awaitSpringApplicationContextReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan) {
        CompletableFuture<ModuleDetail> completableFuture = startModule(moduleName, locationUri, true, mainClass, packageToScan, false);
        return awaitSpringApplicationContextReady(moduleName, completableFuture);
    }

//    public void startSpringModuleSyncWithMainClassLoop(String moduleName, String locationUri, String mainClass) throws ExecutionException, InterruptedException {
//        startSpringModuleSyncWithMainClassLoop(moduleName, locationUri, mainClass, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, String locationUri, String mainClass, String packageToScan) {
        return startModule(moduleName, locationUri, true, mainClass, packageToScan, true);
    }

//    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, String locationUri, String mainClass) {
//        return startSpringModuleAsyncWithMainClassLoop(moduleName, locationUri, mainClass, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, String locationUri, String mainClass, String packageToScan) {
        return startModule(moduleName, locationUri, true, mainClass, packageToScan, false);
    }

    @Override
    public boolean unloadModule(String moduleName) {
        return false;
    }

    //    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, String locationUri, String mainClass) {
//        return startSpringModuleAsyncWithMainClass(moduleName, locationUri, mainClass, "*");
//    }
    @SneakyThrows
    private ModuleDetail awaitModuleReady(String moduleName, CompletableFuture<ModuleDetail> cf) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
        moduleDetail.getReadyLatch().await();
        return moduleDetail;
//        return cf.join();
    }

    private ModuleDetail awaitSpringApplicationContextReady(String moduleName, CompletableFuture<ModuleDetail> completableFuture) {
//        ModuleDetail moduleDetail = completableFuture
//                .exceptionally(t -> {
//                    throw new RuntimeException(t);
//                }).get();
//        return completableFuture.join();
        return awaitModuleReady(moduleName, completableFuture);

//        try {
//            moduleDetail.readyLatch.await();
//        } catch (InterruptedException e) {
//            throw new RuntimeException(e);
//        }
    }

//    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, String locationUri, String mainClass) {
//        return startSpringModuleSyncWithMainClass(moduleName, locationUri, mainClass, "*");
//    }

    public void notifyModuleReady(String moduleName) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
        CountDownLatch readyLatch = moduleDetail.getReadyLatch();
        if (readyLatch != null && readyLatch.getCount() > 0) {
            readyLatch.countDown();
        }
        moduleDetail.setLoadStatus(LoadStatus.LOADED);
    }


}

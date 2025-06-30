package io.github.nhtuan10.modular.module;

import io.github.nhtuan10.modular.annotation.ModularAnnotationProcessor;
import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceLookUpRuntimeException;
import io.github.nhtuan10.modular.api.model.ArtifactLocationType;
import io.github.nhtuan10.modular.api.module.ModuleContext;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.classloader.MavenArtifactsResolver;
import io.github.nhtuan10.modular.context.ModuleContextImpl;
import io.github.nhtuan10.modular.model.ModularServiceHolder;
import io.github.nhtuan10.modular.proxy.ServiceInvocationInterceptor;
import io.github.nhtuan10.modular.serdeserializer.JacksonSmileSerDeserializer;
import io.github.nhtuan10.modular.serdeserializer.JavaSerDeserializer;
import io.github.nhtuan10.modular.serdeserializer.SerDeserializer;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    final Map<Class<?>, Collection<ModularServiceHolder>> loadedModularServices = new ConcurrentHashMap<>(); //UNUSED
    final Map<String, Collection<ModularServiceHolder>> loadedModularServices2 = new ConcurrentHashMap<>();
    //    Map<String, ModularClassLoader> modularClassLoaders = new ConcurrentHashMap<>();
    final Map<Class<?>, List<Object>> loadedProxyObjects = new ConcurrentHashMap<>();
    final Map<String, ModuleDetail> moduleDetailMap = new ConcurrentHashMap<>();
    final SerDeserializer serDeserializer;
    final ModuleLoaderConfiguration configuration;
//    Executor executor;


    private volatile static ModuleLoader instance;
    private static final Object lock = new Object();

    public static ModuleLoader getInstance() {
        return ModuleLoaderImpl.getInstance(ModuleLoaderConfiguration.DEFAULT);
    }


    public static ModuleLoader getInstance(ModuleLoaderConfiguration configuration) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new ModuleLoaderImpl(configuration);
//                    instance.executor = Executors.newFixedThreadPool(configuration.getThreadPoolSize());
                }
            }
        }
        return instance;
    }

    public ModuleLoaderImpl(ModuleLoaderConfiguration configuration) {
        if (configuration.getSerializeType() == ModuleLoaderConfiguration.SerializeType.JAVA) {
            serDeserializer = new JavaSerDeserializer();
        } else {
            serDeserializer = new JacksonSmileSerDeserializer();
        }
        this.configuration = configuration;
    }

    public void loadModule(String name, List<String> locationUris, boolean lazyInit) throws MalformedURLException {
        loadModule(name, locationUris, List.of("*"), lazyInit);
    }

    public void loadModule(String name, List<String> locationUris, List<String> packagesToScan, boolean lazyInit) {
        // Load module
        //TODO: do some validation, check if file not or maven artifacts not exits
        List<URI> mavenUris = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
        for (String location : locationUris) {
            URI uri = URI.create(location);
            log.info("Loading module {} from URI {}", name, uri);
            switch (ArtifactLocationType.valueOf(uri.getScheme().toUpperCase())) {
                case MVN:
                    mavenUris.add(uri);
                    break;
                case FILE:
                    try {
                        Path path = Paths.get(uri);
                        if (path.toFile().exists()) {
                            urls.add(uri.toURL());
                        } else {
                            throw new ModuleLoadRuntimeException("File %s does not exist".formatted(path));
                        }
                    } catch (MalformedURLException e) {
                        throw new ModuleLoadRuntimeException("Error loading module %s from file %s with package %s".formatted(name, uri, packagesToScan), e);
                    }
                    break;
                case HTTP:
                    try {
                        urls.add(uri.toURL());
                    } catch (MalformedURLException e) {
                        throw new ModuleLoadRuntimeException("Error loading module %s from file %s with package %s".formatted(name, uri, packagesToScan), e);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported artifact location type: " + uri.getScheme());
            }
        }
        if (!mavenUris.isEmpty()) {
            urls.addAll(resolveMavenDeps(mavenUris));
        }
        loadModuleFromUrls(name, packagesToScan, lazyInit, urls);


//        synchronized (classLoader) {


//        }
    }

    private List<URL> resolveMavenDeps(List<URI> uris) {
        // Load module from Maven
        List<String> mvnArtifacts = uris.stream().map(uri -> uri.getHost() + uri.getPath().replace("/", ":")).toList();
//        log.info("Loading module from Maven: {}", mvnArtifact);
        return new MavenArtifactsResolver<URL>().resolveMavenDeps(mvnArtifacts, URL.class);
    }

    private void loadModuleFromUrls(String name, List<String> packagesToScan, boolean lazyInit, List<URL> depUrls) {
        ModularClassLoader classLoader = new ModularClassLoader(name, depUrls);
//        classLoader.setExcludedClassPackages(Set.of(MODULAR_ANNOTATION_PKG));
        ModuleDetail moduleDetail = moduleDetailMap.get(name);
        moduleDetail.setClassLoader(classLoader);

        ModularAnnotationProcessor m = new ModularAnnotationProcessor(classLoader);
        try {
            m.annotationProcess(packagesToScan, lazyInit);
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

    private void loadModuleFromFile(String name, URI uri, List<String> packagesToScan, boolean lazyInit) {
        // Load module from file
        log.debug("Loading module {} from file {} with package", name, uri, packagesToScan);
        try {
            loadModuleFromUrls(name, packagesToScan, lazyInit, List.of(uri.toURL()));
        } catch (MalformedURLException e) {
            throw new ModuleLoadRuntimeException("Error loading module %s from file %s with package %s".formatted(name, uri, packagesToScan), e);
        }
    }

    public static ModuleContext getContext() {
        Object moduleLoader;
        try {
            Method m = Class.forName(ModuleLoaderImpl.class.getName(), true, ClassLoader.getSystemClassLoader()).getDeclaredMethod("getInstance");
            m.setAccessible(true);
            moduleLoader = m.invoke(null);
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

    public <I> List<I> getModularServices(Class<I> apiClass, boolean fromSpringAppContext) {
        Collection<ModularServiceHolder> serviceHolders = loadedModularServices2.get(apiClass.getName());
        if (serviceHolders != null) {
            if (!loadedProxyObjects.containsKey(apiClass)) {
                List<I> proxyObjects = serviceHolders.stream()
//                .map(ModularServiceHolder::getServiceClass)
                        .map(serviceHolder -> {
                            try {
                                Object service;
                                if (fromSpringAppContext) {
                                    Class serviceClass = serviceHolder.getServiceClass();
                                    Class serviceAppContextProvide = Class.forName(APPLICATION_CONTEXT_PROVIDER, true, serviceClass.getClassLoader());
                                    service = serviceAppContextProvide.getMethod("getBean", Class.class).invoke(null, serviceClass);
                                } else {
                                    service = serviceHolder.getInstance();
                                }
                                return this.<I>createProxyObject(apiClass, service);
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                     NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
                                throw new ServiceLookUpRuntimeException("Error when getModularServices for class %s".formatted(apiClass.getName()), e);
                            }
                        }).toList();
                loadedProxyObjects.put(apiClass, (List<Object>) proxyObjects);
                return proxyObjects;
            } else {
                return (List<I>) loadedProxyObjects.get(apiClass);
            }
        } else {
            throw new ServiceLookUpRuntimeException("Class '%s' is not registered as a Modular service. Please make sure this class is in the scanned packages and have @ModularService annotation".formatted(apiClass.getName()));
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
    private <I> I createProxyObject(Class<I> apiClass, Object service) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException, NoSuchFieldException {
        ClassLoader apiClassLoader = apiClass.getClassLoader();
        Object svcInvocationInterceptor = apiClassLoader.loadClass(ServiceInvocationInterceptor.class.getName())
                .getConstructor(Object.class, SerDeserializer.class).newInstance(service, serDeserializer );
        Object equalsMethodInterceptor = apiClassLoader.loadClass(ServiceInvocationInterceptor.EqualsMethodInterceptor.class.getName())
                .getConstructor(Object.class).newInstance(service);
        Class<? extends I> c = new ByteBuddy()
                .subclass(apiClass)
//                .name(apiClass.get() + "$Proxy")
                .method(ElementMatchers.isEquals())
                .intercept(MethodDelegation.to(equalsMethodInterceptor))
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isEquals())))
                .intercept(MethodDelegation.to(svcInvocationInterceptor))
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

    private CompletableFuture<ModuleDetail> startModule(String moduleName, List<String> locationUris, boolean lazyInit, String mainClass, List<String> packagesToScan, boolean awaitMainClass) {
        assert (moduleName != null && StringUtils.isNotBlank(moduleName)) : "Module name cannot be null or empty";
        CompletableFuture<ModuleDetail> moduleDetailCompletableFuture = new CompletableFuture<>();
        if (!moduleDetailMap.containsKey(moduleName)) {
            CountDownLatch await = new CountDownLatch(1);
            ModuleDetail moduleDetail = new ModuleDetail(moduleName, LoadStatus.LOADING, null, new CountDownLatch(1), await);
            moduleDetailMap.put(moduleName, moduleDetail);

            Thread t = new Thread(() -> {
                try {
                    loadModule(moduleName, locationUris, packagesToScan, lazyInit);
                    Thread.currentThread().setContextClassLoader(getClassLoader(moduleName));
                    if (mainClass != null) {
                        try {
                            loadClass(moduleName, mainClass).getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[]{});
                            moduleDetailCompletableFuture.complete(moduleDetail);
                            notifyModuleReady(moduleName);
                            log.info("Finish loading module '{}'", moduleName);
                            if (awaitMainClass) {
                                Runtime.getRuntime().addShutdownHook(new Thread(await::countDown));
                                await.await();
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                                 ClassNotFoundException | InterruptedException e) {
                            ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException("Error starting module '%s'".formatted(moduleName), e);
                            moduleDetailCompletableFuture.completeExceptionally(exception);
                            notifyModuleReady(moduleName);
                            throw exception;
                        }
                    } else {
                        moduleDetailCompletableFuture.complete(moduleDetail);
                        notifyModuleReady(moduleName);
                        log.info("Finish loading module '{}'", moduleName);
                    }
                } catch (Exception e) {
                    ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException("Failed to load module '" + moduleName, e);
                    moduleDetailCompletableFuture.completeExceptionally(exception);
                    notifyModuleReady(moduleName);
                    throw exception;
                }
            });
            t.start();
            return moduleDetailCompletableFuture;
        } else {
            ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException("Module '" + moduleName + "' is already loaded");
            moduleDetailCompletableFuture.completeExceptionally(exception);
            notifyModuleReady(moduleName);
            throw exception;
        }
    }

//    static interface StartModuleTask extends Runnable{
//        void run() throws Exception;
//    }

    @Override
    public ModuleDetail startModuleSync(String moduleName, List<String> locationUris, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, false, null, packagesToScan, false);
//        return cf.join();
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, false, mainClass, packagesToScan, false);
//        return cf.join();
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, List<String> locationUris, List<String> packagesToScan) {
        return startModule(moduleName, locationUris, false, null, packagesToScan, false);
    }

//    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, String locationUri) {
//        return startModuleAsync(moduleName, locationUris, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, false, mainClass, packageToScan, false);
    }

    // Spring

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, true, mainClass, packageToScan, true);
        return awaitSpringApplicationContextReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        CompletableFuture<ModuleDetail> completableFuture = startModule(moduleName, locationUris, true, mainClass, packageToScan, false);
        return awaitSpringApplicationContextReady(moduleName, completableFuture);
    }

//    public void startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass) throws ExecutionException, InterruptedException {
//        startSpringModuleSyncWithMainClassLoop(moduleName, locationUris, mainClass, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, true, mainClass, packageToScan, true);
    }

//    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass) {
//        return startSpringModuleAsyncWithMainClassLoop(moduleName, locationUris, mainClass, "*");
//    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, true, mainClass, packageToScan, false);
    }

    @Override
    public boolean unloadModule(String moduleName) {
        return false;
    }

    //    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass) {
//        return startSpringModuleAsyncWithMainClass(moduleName, locationUris, mainClass, "*");
//    }

    private ModuleDetail awaitModuleReady(String moduleName, CompletableFuture<ModuleDetail> cf) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
//        cf.exceptionally((throwable) -> {
//            moduleDetail.setLoadStatus(LoadStatus.FAILED);
//            if (throwable instanceof RuntimeException runtimeException) {
//                throw runtimeException;
//            }else {
//                throw new RuntimeException(throwable);
//            }
//        });
        try {
            moduleDetail.getReadyLatch().await();
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while waiting for module %s ready".formatted(moduleName), e);
        }
        if (cf.isCompletedExceptionally()) {
            moduleDetail.setLoadStatus(LoadStatus.FAILED);
            cf.join();
        } else {
            moduleDetail.setLoadStatus(LoadStatus.LOADED);
        }

        return moduleDetail;
//        return cf.join();
    }

    private ModuleDetail awaitSpringApplicationContextReady(String
                                                                    moduleName, CompletableFuture<ModuleDetail> completableFuture) {
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

//    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass) {
//        return startSpringModuleSyncWithMainClass(moduleName, locationUris, mainClass, "*");
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

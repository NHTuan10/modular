package io.github.nhtuan10.modular.impl.module;

import io.github.nhtuan10.modular.api.exception.AnnotationProcessingRuntimeException;
import io.github.nhtuan10.modular.api.exception.DuplicatedModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceLookUpRuntimeException;
import io.github.nhtuan10.modular.api.model.ArtifactLocationType;
import io.github.nhtuan10.modular.api.module.ExternalContainer;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.impl.annotation.ModularAnnotationProcessor;
import io.github.nhtuan10.modular.impl.classloader.MavenArtifactsResolver;
import io.github.nhtuan10.modular.impl.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.impl.model.ModularServiceHolder;
import io.github.nhtuan10.modular.impl.proxy.ProxyCreator;
import io.github.nhtuan10.modular.impl.serdeserializer.JacksonSmileSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.JavaSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.KryoSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

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
public class DefaultModuleLoader implements ModuleLoader {
    public static final String APPLICATION_CONTEXT_PROVIDER = "io.github.nhtuan10.modular.spring.ApplicationContextProvider";
    public static final String PROXY_TARGET_FIELD_NAME = "target";

    final Map<String, Collection<ModularServiceHolder>> loadedModularServices2 = new ConcurrentHashMap<>();
    //    final Map<Class<?>, List<?>> loadedProxyObjects = new ConcurrentHashMap<>();
//    final Map<ProxyCacheKey, List<?>> loadedProxyObjects = new ConcurrentHashMap<>();
    final Map<ProxyCacheKey, Object> loadedProxyObjects = new ConcurrentHashMap<>();
    final Map<String, ModuleDetail> moduleDetailMap = new ConcurrentHashMap<>();
    final SerDeserializer serDeserializer;
    final ModuleLoaderConfiguration configuration;
    private volatile static ModuleLoader instance;
    private static final Object lock = new Object();

    public static ModuleLoader getInstance() {
        return DefaultModuleLoader.getInstance(ModuleLoaderConfiguration.DEFAULT);
    }


    public static ModuleLoader getInstance(ModuleLoaderConfiguration configuration) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DefaultModuleLoader(configuration);
                }
            }
        }
        return instance;
    }

    public DefaultModuleLoader(ModuleLoaderConfiguration configuration) {
        serDeserializer = switch (configuration.getSerializeType()) {
            case JAVA -> new JavaSerDeserializer();
            case JACKSON_SMILE -> new JacksonSmileSerDeserializer();
            case KRYO -> new KryoSerDeserializer();
        };
        this.configuration = configuration;
    }

    public void loadModule(String name, ModuleLoadConfiguration moduleLoadConfiguration) {
        // Load module
        List<URI> mavenUris = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
        for (String location : moduleLoadConfiguration.locationUris()) {
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
                            throw new ModuleLoadRuntimeException(name, "Error loading module %s. File %s does not exist".formatted(name, path));
                        }
                    } catch (MalformedURLException e) {
                        throw new ModuleLoadRuntimeException(name, "Error loading module %s from file %s with package %s".formatted(name, uri, moduleLoadConfiguration.packagesToScan()), e);
                    }
                    break;
                case HTTP:
                    try {
                        urls.add(uri.toURL());
                    } catch (MalformedURLException e) {
                        throw new ModuleLoadRuntimeException(name, "Error loading module %s from file %s with package %s".formatted(name, uri, moduleLoadConfiguration.packagesToScan()), e);
                    }
                    break;
                default:
                    throw new ModuleLoadRuntimeException(name, "Unsupported artifact location type: " + uri.getScheme());
            }
        }
        if (!mavenUris.isEmpty()) {
            urls.addAll(resolveMavenDeps(name, mavenUris));
        }
        loadModuleFromUrls(name, moduleLoadConfiguration, urls);
    }

    private List<URL> resolveMavenDeps(String moduleName, List<URI> uris) {
        // Load module from Maven
        List<String> mvnArtifacts = uris.stream().map(uri -> uri.getHost() + uri.getPath().replace("/", ":")).toList();
        log.info("Loading module {} from Maven artifacts: {}", moduleName, mvnArtifacts);
        return new MavenArtifactsResolver<URL>().resolveDependencies(mvnArtifacts, URL.class);
    }

    private void loadModuleFromUrls(String name, ModuleLoadConfiguration moduleLoadConfiguration, List<URL> depUrls) {
        ModularClassLoader moduleClassLoader = new ModularClassLoader(name, depUrls);
        ModuleDetail moduleDetail = moduleDetailMap.get(name);
        moduleDetail.setClassLoader(moduleClassLoader);
//        addAllOpens(ModuleLoaderImpl.class.getClassLoader());
//        addAllOpens(moduleClassLoader);
        ModularAnnotationProcessor m = new ModularAnnotationProcessor(moduleClassLoader);
        try {
            Map<Class<?>, Collection<ModularServiceHolder>> modularServices = m.annotationProcess(name, moduleLoadConfiguration);
            addModularServices(modularServices);
        } catch (Exception e) {
            throw new AnnotationProcessingRuntimeException(name, "Fail during annotation processing", e);
        }
    }

    private void addModularServices(Map<Class<?>, Collection<ModularServiceHolder>> container) {
        container.forEach((key, value) -> {
            loadedModularServices2.putIfAbsent(key.getName(), Collections.synchronizedSet(new LinkedHashSet<>()));
            loadedModularServices2.get(key.getName()).addAll(value);
        });
    }

    @SneakyThrows
    private void addAllOpens(ClassLoader classLoader) {
        final Module unnamedModule = classLoader.getUnnamedModule();
        final Method method = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
        method.setAccessible(true);

        ModuleLayer.boot().modules().forEach(module -> {
            final Set<String> packages = module.getPackages();
            for (String eachPackage : packages) {
                try {
                    method.invoke(module, eachPackage, unnamedModule, true, true);
                } catch (Exception e) {
                    log.error("Error when add-opens {}/{}={}", module.getName(), eachPackage, unnamedModule.toString(), e);
                }
                log.info("--add-open " + module.getName() + "/" + eachPackage + "=" + unnamedModule.toString());
            }
        });
    }

    public Class<?> loadClass(String module, String name) throws ClassNotFoundException {
        return moduleDetailMap.get(module).getClassLoader().loadClass(name);
    }

    public ClassLoader getClassLoader(String module) {
        return moduleDetailMap.get(module).getClassLoader();
    }

    @Override
    public <I> List<I> getModularServices(Class<I> clazz) {
        return getModularServices(clazz, true);
    }

    @Override
    public <I> List<I> getModularServices(Class<I> clazz, String moduleName) {
        return getModularServices(clazz, moduleName, null, null, true);
    }

    @Override
    public <I> List<I> getModularServices(Class<I> clazz, boolean copyTransClassLoaderObjects) {
        return getModularServices(clazz, null, null, copyTransClassLoaderObjects);
    }

    @Override
    public <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer, boolean copyTransClassLoaderObjects) {
        return getModularServices(clazz, moduleName, externalContainer, name, copyTransClassLoaderObjects);
    }

    @Override
    public <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz) {
        return getModularServicesFromSpring(name, clazz, true);
    }

    @Override
    public <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, boolean copyTransClassLoaderObjects) {
        return getModularServices(clazz, ExternalContainer.SPRING, name, copyTransClassLoaderObjects);
    }

    public <I> List<I> getModularServices(Class<I> apiClass, ExternalContainer externalContainer, String beanName, boolean copyTransClassLoaderObjects) {
        return getModularServices(apiClass, null, externalContainer, beanName, copyTransClassLoaderObjects);
    }

    <I> List<I> getModularServices(Class<I> apiClass, String moduleName, ExternalContainer externalContainer, String beanName, boolean copyTransClassLoaderObjects) {
        //TODO: need to refactor this code to support another DI or external container rather than Spring
        Collection<ModularServiceHolder> serviceHolders = loadedModularServices2.get(apiClass.getName());
        if (serviceHolders != null) {
//
//            List<I> proxyObjects = (List<I>) loadedProxyObjects.computeIfAbsent(apiClass, clazz ->
//            List<I> proxyObjects = (List<I>) loadedProxyObjects.computeIfAbsent(new ProxyCacheKey(apiClass, externalContainer, beanName), proxyCacheKey -> {
//                Class<?> clazz = proxyCacheKey.apiClass();
            @SuppressWarnings("unchecked")
            List<I> proxyObjects = (List<I>) serviceHolders.stream().filter(sh -> (moduleName == null || sh.getModuleName().equals(moduleName))).map(serviceHolder -> {
                try {
                    if (serviceHolder.getExternalContainer() != externalContainer) { // continue
                        return null;
                    }
                    Object service;
                    if (externalContainer == ExternalContainer.SPRING) {
                        Class<?> serviceClass = serviceHolder.getServiceClass();
                        Class<?> serviceAppContextProvide = Class.forName(APPLICATION_CONTEXT_PROVIDER, true, serviceClass.getClassLoader());
                        try {
                            if (StringUtils.isNotBlank(beanName)) {
                                if (beanName.equals(serviceHolder.getExternalBeanName())) {
                                    service = serviceAppContextProvide.getMethod("getBean", String.class).invoke(null, beanName);
                                } else {
                                    return null;
                                }
//                                    catch (InvocationTargetException ex){
//                                        if ("org.springframework.beans.factory.NoSuchBeanDefinitionException".equals(ex.getTargetException().getClass().getName())){
//                                            return null;
//                                        }
//                                        else {
//                                            throw new ServiceLookUpRuntimeException("Error when getModularServices from Spring Application Context for class %s with name %s".formatted(clazz.getName(), beanName),ex);
//                                        }
//                                    }
                            } else {
//                                        service = serviceAppContextProvide.getMethod("getBean", Class.class).invoke(null, serviceClass);
                                service = serviceAppContextProvide.getMethod("getBean", String.class).invoke(null, serviceHolder.getExternalBeanName());
                            }
                        } catch (Exception ex) {
                            throw new ServiceLookUpRuntimeException("Error when getModularServices from Spring Application Context for class %s with name %s".formatted(apiClass.getName(), beanName), ex);
                        }
                        serviceHolder.setExternalBeanName(beanName);
                        serviceHolder.setServiceClass(service.getClass());
                        serviceHolder.setInstance(service);
                    } else if (externalContainer == null) {
                        service = serviceHolder.getInstance();
                    } else {
                        throw new ServiceLookUpRuntimeException("Unsupported external container: " + externalContainer);
                    }
                    if (service != null) {
                        return loadedProxyObjects.computeIfAbsent(new ProxyCacheKey(apiClass, service), proxyCacheKey -> {
                            try {
                                return ProxyCreator.createProxyObject(apiClass, service, this.serDeserializer, copyTransClassLoaderObjects, apiClass.getClassLoader(), serviceHolder.getClassLoader());
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                     NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
                                throw new ServiceLookUpRuntimeException("Error when getModularServices for class %s".formatted(apiClass.getName()), e);
                            }
                        });
                    } else
                        return null;
                } catch (
                        ClassNotFoundException e) {
                    throw new ServiceLookUpRuntimeException("Error when getModularServices for class %s".formatted(apiClass.getName()), e);
                }
            }).filter(Objects::nonNull).toList();
//            };
            return proxyObjects;
        } else {
            throw new ServiceLookUpRuntimeException("Class '%s' is not registered as a Modular service. Please make sure this class is in the scanned packages and have @ModularService annotation".formatted(apiClass.getName()));
        }
    }

    public static record ProxyCacheKey(Class<?> apiClass, Object service) {
    }

    private CompletableFuture<ModuleDetail> startModule(String moduleName, List<String> locationUris, ExternalContainer externalContainer, String mainClass, List<String> packagesToScan, boolean awaitMainClass) {
        ModuleLoadConfiguration config = ModuleLoadConfiguration.builder()
                .locationUris(locationUris)
                .externalContainer(externalContainer)
                .mainClass(mainClass)
                .packagesToScan(packagesToScan)
                .allowNonAnnotatedServices(false)
                .awaitMainClass(awaitMainClass)
                .build();
        return startModule(moduleName, config);
    }

    private CompletableFuture<ModuleDetail> startModule(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        assert (StringUtils.isNotBlank(moduleName)) : "Module name cannot be null or empty";
        final String FINISH_LOADING_MSG = "Finish loading module '{}'";
        CompletableFuture<ModuleDetail> moduleDetailCompletableFuture = new CompletableFuture<>();
        if (!moduleDetailMap.containsKey(moduleName)) {
            CountDownLatch await = new CountDownLatch(1);
            ModuleDetail moduleDetail = new ModuleDetail(moduleName, LoadStatus.LOADING, null, new CountDownLatch(1), await);
            moduleDetailMap.put(moduleName, moduleDetail);

            Thread t = new Thread(() -> {
                try {
                    loadModule(moduleName, moduleLoadConfiguration);
                    Thread.currentThread().setContextClassLoader(getClassLoader(moduleName));
                    if (moduleLoadConfiguration.mainClass() != null) {
                        try {
                            loadClass(moduleName, moduleLoadConfiguration.mainClass()).getDeclaredMethod("main", String[].class).invoke(null, (Object) new String[]{});
                            moduleDetailCompletableFuture.complete(moduleDetail);
                            notifyModuleReady(moduleName);
                            log.info(FINISH_LOADING_MSG, moduleName);
                            if (moduleLoadConfiguration.awaitMainClass()) {
                                Runtime.getRuntime().addShutdownHook(new Thread(await::countDown));
                                await.await();
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                                 ClassNotFoundException | InterruptedException e) {
                            ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException(moduleName, "Error starting module '%s'".formatted(moduleName), e);
                            moduleDetailCompletableFuture.completeExceptionally(exception);
                            notifyModuleReady(moduleName);
                            throw exception;
                        }
                    } else {
                        moduleDetailCompletableFuture.complete(moduleDetail);
                        notifyModuleReady(moduleName);
                        log.info(FINISH_LOADING_MSG, moduleName);
                    }
                } catch (Exception e) {
                    ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException(moduleName, "Failed to load module '" + moduleName, e);
                    moduleDetailCompletableFuture.completeExceptionally(exception);
                    notifyModuleReady(moduleName);
                    throw exception;
                }
            });
            t.start();
            return moduleDetailCompletableFuture;
        } else {
            DuplicatedModuleLoadRuntimeException exception = new DuplicatedModuleLoadRuntimeException(moduleName, "Module '" + moduleName + "' is already loaded");
            moduleDetailCompletableFuture.completeExceptionally(exception);
            notifyModuleReady(moduleName);
            throw exception;
        }
    }

    @Override
    public ModuleDetail startModuleSync(String moduleName, List<String> locationUris, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, null, null, packagesToScan, false);
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, null, mainClass, packagesToScan, false);
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, List<String> locationUris, List<String> packagesToScan) {
        return startModule(moduleName, locationUris, null, null, packagesToScan, false);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, null, mainClass, packageToScan, false);
    }

    // Spring

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, true);
        return awaitSpringApplicationContextReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        CompletableFuture<ModuleDetail> completableFuture = startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, false);
        return awaitSpringApplicationContextReady(moduleName, completableFuture);
    }

    @Override
    public ModuleDetail startModuleSync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        CompletableFuture<ModuleDetail> completableFuture = startModule(moduleName, moduleLoadConfiguration);
        return awaitSpringApplicationContextReady(moduleName, completableFuture);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleASync(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        return startModule(moduleName, moduleLoadConfiguration);
    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, true);
    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<String> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, false);
    }

    @Override
    public boolean unloadModule(String moduleName) {
        return false;
    }

    private ModuleDetail awaitModuleReady(String moduleName, CompletableFuture<ModuleDetail> cf) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
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
    }

    private ModuleDetail awaitSpringApplicationContextReady(String
                                                                    moduleName, CompletableFuture<ModuleDetail> completableFuture) {
        return awaitModuleReady(moduleName, completableFuture);
    }

    public void notifyModuleReady(String moduleName) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
        CountDownLatch readyLatch = moduleDetail.getReadyLatch();
        if (readyLatch != null && readyLatch.getCount() > 0) {
            readyLatch.countDown();
        }
        moduleDetail.setLoadStatus(LoadStatus.LOADED);
    }


}

package io.github.nhtuan10.modular.impl.module;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nhtuan10.modular.api.Modular;
import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.AnnotationProcessingRuntimeException;
import io.github.nhtuan10.modular.api.exception.DuplicatedModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceLookUpRuntimeException;
import io.github.nhtuan10.modular.api.model.ArtifactLocationType;
import io.github.nhtuan10.modular.api.module.EntryPointResultWrapper;
import io.github.nhtuan10.modular.api.module.ExternalContainer;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.api.module.ModuleLoader;
import io.github.nhtuan10.modular.entry.ModularEntryPoint;
import io.github.nhtuan10.modular.impl.annotation.ModularAnnotationProcessor;
import io.github.nhtuan10.modular.impl.classloader.DefaultModularClassLoader;
import io.github.nhtuan10.modular.impl.classloader.MavenArtifactsResolver;
import io.github.nhtuan10.modular.impl.model.ModularServiceHolder;
import io.github.nhtuan10.modular.impl.proxy.ServiceProxyCreator;
import io.github.nhtuan10.modular.impl.serdeserializer.JavaSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.KryoSerDeserializer;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import io.github.nhtuan10.modular.impl.util.Utils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DefaultModuleLoader implements ModuleLoader {
    public static final String APPLICATION_CONTEXT_PROVIDER = "io.github.nhtuan10.modular.spring.ApplicationContextProvider";
    public static final String PROXY_TARGET_FIELD_NAME = "target";
    public static final String WEB_INF = "WEB-INF";
    public static final String BOOT_INF = "BOOT-INF";
    public static final String SUCCESSFULLY_UNLOADED_MODULE_LOG = "Successfully unloaded module {}";

    final Map<String, Collection<ModularServiceHolder>> loadedModularServices2 = new ConcurrentHashMap<>();
    //    final Map<Class<?>, List<?>> loadedProxyObjects = new ConcurrentHashMap<>();
//    final Map<ProxyCacheKey, List<?>> loadedProxyObjects = new ConcurrentHashMap<>();
    final Map<ProxyCacheKey, Object> loadedProxyObjects = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, ModuleDetail> moduleDetailMap = new ConcurrentHashMap<>();
    final SerDeserializer serDeserializer;
    final ModuleLoaderConfiguration configuration;
    private volatile static ModuleLoader instance;
    private static final Object lock = new Object();
    private static final Object moduleLoadingLock = new Object();
    final Map<String, ModularClassLoader> modulerClassLoaderMap = new ConcurrentHashMap<>();
    final ThreadLocal<String> currentModuleNameThreadLocal = new ThreadLocal<>();
    private final ObjectMapper objectMapper;

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
        switch (configuration.getSerializeType()) {
            case JAVA:
                serDeserializer = new JavaSerDeserializer();
                break;
//            case JACKSON_SMILE -> new JacksonSmileSerDeserializer();
            case KRYO:
            default:
                serDeserializer = new KryoSerDeserializer();
                break;
        }
        this.configuration = configuration;
        objectMapper = new ObjectMapper();
    }

    public void loadModule(String name, ModuleLoadConfiguration moduleLoadConfiguration) {
        // Load module
        List<URI> mavenUris = new ArrayList<>();
        List<URL> urls = new ArrayList<>();
        for (URI uri : moduleLoadConfiguration.locationUris()) {
            log.info("Loading module {} from URI {}", name, uri);
            switch (ArtifactLocationType.valueOf(uri.getScheme().toUpperCase())) {
                case MVN:
                    mavenUris.add(uri);
                    break;
                case FILE:
                    urls.addAll(buildUris(name, uri, moduleLoadConfiguration.packagesToScan(), null).stream().flatMap(innerUri -> {
                        try {
                            if (innerUri.getPath().endsWith(".war")) {
                                Path warFile = Paths.get(innerUri.getPath());
                                Path targetDir = Paths.get(moduleLoadConfiguration.workingDir()).resolve(warFile.getFileName().toString().replace(".war", ""));
                                Utils.extractCompressedFile(warFile, targetDir);
                                List<URI> dependencies = buildUris(name, targetDir.resolve(WEB_INF + "/lib/*").toUri(), moduleLoadConfiguration.packagesToScan(), null);
                                dependencies.add(0, targetDir.resolve(WEB_INF + "/classes/").toUri());
                                return dependencies.stream().map(u -> {
                                    try {
                                        return u.toURL();
                                    } catch (MalformedURLException e) {
                                        throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, u, moduleLoadConfiguration.packagesToScan()), e);
                                    }
                                });
                            } else {
                                return Stream.of(innerUri.toURL());
                            }
                        } catch (MalformedURLException e) {
                            throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, innerUri, moduleLoadConfiguration.packagesToScan()), e);
                        } catch (IOException e) {
                            throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from WAR file %s with package %s", name, innerUri, moduleLoadConfiguration.packagesToScan()), e);
                        }
                    }).collect(Collectors.toList()));
                    break;
                case JAR:
                    try {
                        String[] s = uri.getSchemeSpecificPart().split("!", 2);
                        if (s.length >= 1) {
                            URI fileUri = new URI(s[0]);
                            String suffix = (s.length > 1) ? ("!" + s[1]) : null;
                            List<URL> scannedUrls = buildUris(name, fileUri, moduleLoadConfiguration.packagesToScan(), suffix).stream()
                                    .map(innerUri ->
                                    {
                                        try {
                                            return new URI("jar", innerUri.toString(), null).toURL();
                                        } catch (MalformedURLException | URISyntaxException e) {
                                            throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, uri, moduleLoadConfiguration.packagesToScan()), e);
                                        }
                                    }).collect(Collectors.toList());
                            urls.addAll(scannedUrls);
                        }
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case SPRINGBOOT:
                    if (uri.getPath().endsWith(".jar")) {
                        Path warFile = Paths.get(uri.getPath());
                        Path targetDir = Paths.get(moduleLoadConfiguration.workingDir()).resolve(warFile.getFileName().toString().replace(".jar", ""));
                        try {
                            Utils.extractCompressedFile(warFile, targetDir);
                            List<URI> dependencies = buildUris(name, targetDir.resolve(BOOT_INF + "/lib/*").toUri(), moduleLoadConfiguration.packagesToScan(), null);
                            dependencies.add(0, targetDir.resolve(BOOT_INF + "/classes/").toUri());
                            urls.addAll(dependencies.stream().map(u -> {
                                try {
                                    return u.toURL();
                                } catch (MalformedURLException e) {
                                    throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, u, moduleLoadConfiguration.packagesToScan()), e);
                                }
                            }).collect(Collectors.toList()));
                        } catch (IOException e) {
                            throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from Spring Boot jar file %s with package %s", name, uri, moduleLoadConfiguration.packagesToScan()), e);
                        }
                    } else {
                        throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s, please provide a Spring Boot jar file, given: %s", name, uri));
                    }
                    break;
                case HTTP:
                default:
                    try {
                        urls.add(uri.toURL());
                    } catch (MalformedURLException e) {
                        throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, uri, moduleLoadConfiguration.packagesToScan()), e);
                    }
                    break;
            }
        }
        if (!mavenUris.isEmpty()) {
            urls.addAll(resolveMavenDeps(name, mavenUris));
        }
        loadModuleFromUrls(name, moduleLoadConfiguration, urls);
    }

    private List<URI> buildUris(String name, URI parentUri, List<String> packages, String suffix) {
        List<URI> uris = new ArrayList<>();
        Path path = Paths.get(parentUri);
        if (path.toFile().exists()) {
            try {
                uris.add(new URI(parentUri + (suffix != null ? suffix : "")));
            } catch (URISyntaxException e) {
                throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, parentUri, packages), e);
            }
        } else {
            List<String> scannedList = Utils.listFiles(parentUri);
            if (scannedList.isEmpty()) {
                log.warn("Loading module {}. File {} does not exist", name, parentUri);
            } else {
                List<URI> scannedUriList = scannedList.stream().map(u -> {
                    try {
                        String f = suffix != null ? u + suffix : u;
                        return new URI("file", f, null);
                    } catch (URISyntaxException e) {
                        throw new ModuleLoadRuntimeException(name, String.format("Error loading module %s from file %s with package %s", name, u, packages), e);
                    }
                }).collect(Collectors.toList());
                uris.addAll(scannedUriList);
            }
        }
        return uris;
    }

    private List<URL> resolveMavenDeps(String moduleName, List<URI> uris) {
        // Load module from Maven
        List<String> mvnArtifacts = uris.stream().map(URI::getSchemeSpecificPart).collect(Collectors.toList());
        log.info("Loading module {} from Maven artifacts: {}", moduleName, mvnArtifacts);
        return new MavenArtifactsResolver<URL>().resolveDependencies(mvnArtifacts, URL.class);
    }

    private void loadModuleFromUrls(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration, List<URL> depUrls) {
        currentModuleNameThreadLocal.set(moduleName);
        ModularClassLoader moduleClassLoader;
        String classLoaderNameFromConfig = moduleLoadConfiguration.modularClassLoaderName();
        if (StringUtils.isNotBlank(classLoaderNameFromConfig)) {
            moduleClassLoader = modulerClassLoaderMap.computeIfAbsent(classLoaderNameFromConfig, classLoaderName ->
                    new DefaultModularClassLoader(classLoaderNameFromConfig, List.of(moduleName), moduleLoadConfiguration.parentClassLoader(), moduleLoadConfiguration.prefixesLoadedBySystemClassLoader(), moduleLoadConfiguration.doesIncludeSystemClasspath()));
            moduleClassLoader.addClassPathUrls(depUrls);
            moduleClassLoader.addModule(moduleName);
        } else if (moduleLoadConfiguration.modularClassLoader() != null) {
            moduleClassLoader = moduleLoadConfiguration.modularClassLoader();
        } else {
            moduleClassLoader = new DefaultModularClassLoader(List.of(moduleName), depUrls, moduleLoadConfiguration.parentClassLoader(), moduleLoadConfiguration.prefixesLoadedBySystemClassLoader(), moduleLoadConfiguration.doesIncludeSystemClasspath());
            modulerClassLoaderMap.put(moduleName, moduleClassLoader);
        }
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
        moduleDetail.setClassLoader(moduleClassLoader);
//        addAllOpens(ModuleLoaderImpl.class.getClassLoader());
//        addAllOpens(moduleClassLoader);
        ModularAnnotationProcessor m = new ModularAnnotationProcessor(moduleClassLoader);
        try {
            Map<Class<?>, Collection<ModularServiceHolder>> modularServices = m.annotationProcess(moduleName, moduleLoadConfiguration);
            addModularServices(modularServices);
        } catch (Exception e) {
            throw new AnnotationProcessingRuntimeException(moduleName, "Fail during annotation processing", e);
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
                log.info("--add-open {}/{}={}", module.getName(), eachPackage, unnamedModule.toString());
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
    public <I> List<I> getModularServices(String name, Class<I> clazz, ExternalContainer externalContainer) {
        return getModularServices(clazz, null, externalContainer, name, true);
    }

    @Override
    public <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer) {
        return getModularServices(clazz, moduleName, externalContainer, name, true);
    }

    @Override
    public <I> List<I> getModularServices(String name, Class<I> clazz, String moduleName, ExternalContainer externalContainer, boolean copyTransClassLoaderObjects) {
        return getModularServices(clazz, moduleName, externalContainer, name, copyTransClassLoaderObjects);
    }

    @Override
    public <I> List<I> getModularServicesFromSpring(String name, Class<I> clazz, String moduleName) {
        return getModularServices(clazz, ExternalContainer.SPRING, name, false);
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
                            throw new ServiceLookUpRuntimeException(String.format("Error when getModularServices from Spring Application Context for class %s with name %s", apiClass.getName(), beanName), ex);
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
                                return ServiceProxyCreator.createProxyObject(apiClass, service, this.serDeserializer, copyTransClassLoaderObjects, apiClass.getClassLoader(), serviceHolder.getClassLoader());
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                                     NoSuchMethodException | ClassNotFoundException | NoSuchFieldException e) {
                                throw new ServiceLookUpRuntimeException(String.format("Error when getModularServices for class %s", apiClass.getName()), e);
                            }
                        });
                    } else
                        return null;
                } catch (
                        ClassNotFoundException e) {
                    throw new ServiceLookUpRuntimeException(String.format("Error when getModularServices for class %s", apiClass.getName()), e);
                }
            }).filter(Objects::nonNull).collect(Collectors.toList());
//            };
            return proxyObjects;
        } else {
            throw new ServiceLookUpRuntimeException(String.format("Class '%s' is not registered as a Modular service. Please make sure this class is in the scanned packages and have @ModularService annotation", apiClass.getName()));
        }
    }

    @RequiredArgsConstructor
    @EqualsAndHashCode
    @ToString
    public static final class ProxyCacheKey {
        private final Class<?> apiClass;
        private final Object service;

        public Class<?> apiClass() {
            return apiClass;
        }

        public Object service() {
            return service;
        }

    }

    private CompletableFuture<ModuleDetail> startModule(String moduleName, List<URI> locationUris, ExternalContainer externalContainer, String mainClass, List<String> packagesToScan, boolean awaitMainClass) {
        ModuleLoadConfiguration config = ModuleLoadConfiguration.builder()
                .locationUris(locationUris)
                .externalContainer(externalContainer)
                .mainClass(mainClass)
                .packagesToScan(packagesToScan)
                .allowNonAnnotatedServices(false)
                .awaitModule(awaitMainClass)
                .doesIncludeSystemClasspath(false)
                .build();
        return startModule(moduleName, config);
    }

    private CompletableFuture<ModuleDetail> startModule(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        assert (StringUtils.isNotBlank(moduleName)) : "Module name cannot be null or empty";
        CompletableFuture<ModuleDetail> moduleDetailCompletableFuture = new CompletableFuture<>();
        if (moduleDetailMap.containsKey(moduleName)) {
            if (moduleLoadConfiguration.isOverride()) {
                try {
                    unloadModule(moduleName);
                } catch (ModuleLoadRuntimeException e) {
                    moduleDetailCompletableFuture.completeExceptionally(e);
                    notifyModuleReady(moduleName);
                    throw e;
                }
            } else {
                throw duplicatedModuleException(moduleName, moduleDetailCompletableFuture);
            }
        }
        AtomicBoolean allowToLoad = new AtomicBoolean(false);
        moduleDetailMap.computeIfAbsent(moduleName, m -> {
            allowToLoad.set(true);
            CountDownLatch await = new CountDownLatch(1);
            //            moduleDetailMap.put(moduleName, moduleDetail);
            return new ModuleDetail(moduleName, moduleLoadConfiguration, LoadStatus.LOADING, null, new CountDownLatch(1), await, null, null, null);
        });
        if (allowToLoad.get()) {
            ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
            CountDownLatch await = moduleDetail.getAwaitMainClassLatch();
//            CompletableFuture.runAsync(() -> {
            Thread t = new Thread(() -> {
                try {

                    loadModule(moduleName, moduleLoadConfiguration);
                    Thread.currentThread().setContextClassLoader(getClassLoader(moduleName));

                    if ((moduleLoadConfiguration.mainClass() != null) || (moduleLoadConfiguration.entryPointClass() != null)) {
                        try {
                            if (moduleLoadConfiguration.entryPointClass() != null) {
                                EntryPointResultWrapper entryPointResultWrapper = handleModuleEntryPoint(moduleName, moduleLoadConfiguration.entryPointClass(), moduleLoadConfiguration.entryPointArgumentInJsonString(), moduleLoadConfiguration.entryPointArgument(), moduleLoadConfiguration.executeEntryPointWhenLoaded());
                                moduleDetail.setModularEntryPoint(entryPointResultWrapper.getModularEntryPoint());
                                moduleDetail.setEntryPointResult(entryPointResultWrapper.getEntryPointResult());
                                if (entryPointResultWrapper != null && entryPointResultWrapper.getEntryPointResult() != null) {
                                    moduleDetail.setEntryPointResultInJsonString(objectMapper.writeValueAsString(entryPointResultWrapper.getEntryPointResult()));
                                }
                            }
                            if (moduleLoadConfiguration.mainClass() != null) {
                                loadClass(moduleName, moduleLoadConfiguration.mainClass()).getDeclaredMethod("main", String[].class).invoke(null, (Object) moduleLoadConfiguration.mainMethodArguments());
                            }
                            finishLoading(moduleName, moduleDetailCompletableFuture, moduleDetail);
                            if (moduleLoadConfiguration.awaitModule()) {
                                Runtime.getRuntime().addShutdownHook(new Thread(await::countDown));
                                await.await();
                            }
                        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException |
                                 ClassNotFoundException | InterruptedException e) {
                            Throwable cause = e;
                            if (e instanceof InvocationTargetException) {
                                cause = ((InvocationTargetException) e).getTargetException();
                            }
                            ModuleLoadRuntimeException exception = new ModuleLoadRuntimeException(moduleName, String.format("Error starting module '%s'", moduleName), cause);
                            moduleDetailCompletableFuture.completeExceptionally(exception);
                            notifyModuleReady(moduleName);
                            throw exception;
                        }
                    } else {
                        finishLoading(moduleName, moduleDetailCompletableFuture, moduleDetail);
                    }
                } catch (Exception e) {
                    ModuleLoadRuntimeException exception;
                    if (e instanceof ModuleLoadRuntimeException) {
                        exception = (ModuleLoadRuntimeException) e;
                    } else {
                        exception = new ModuleLoadRuntimeException(moduleName, "Failed to load module '" + moduleName, e);
                    }
                    moduleDetailCompletableFuture.completeExceptionally(exception);
                    notifyModuleReady(moduleName);
                    throw exception;
                }
            });
            t.start();
            return moduleDetailCompletableFuture;
        } else {
            throw duplicatedModuleException(moduleName, moduleDetailCompletableFuture);
        }
    }

    public Object invokeModuleEntryPoint(String moduleName, String entryPointClass, String entryPointArgumentInJsonString, Object entryPointArgument) throws Exception {
        return handleModuleEntryPoint(moduleName, entryPointClass, entryPointArgumentInJsonString, entryPointArgument, true).getEntryPointResult();
    }

    public EntryPointResultWrapper handleModuleEntryPoint(String moduleName, String entryPointClass, String entryPointArgumentInJsonString, Object entryPointArgument, boolean doesExecute) throws Exception {
        List<ModularEntryPoint> foundEntryPoints = getModularEntryPoints(moduleName, entryPointClass);
        if (foundEntryPoints.size() == 1) {
            AtomicReference<EntryPointResultWrapper> result = new AtomicReference<>();
            ModularEntryPoint<?, ?> modularEntryPoint = foundEntryPoints.get(0);
            if (doesExecute) {
                Object targetEntryPointObject = getTargetFieldFromProxyObject(modularEntryPoint);
                String paramStr = entryPointArgumentInJsonString != null ? entryPointArgumentInJsonString :
                        (entryPointArgument != null ? objectMapper.writeValueAsString(entryPointArgument) : null);
                Type[] interfaces = targetEntryPointObject.getClass().getGenericInterfaces();
                Arrays.stream(interfaces).filter(i -> (i instanceof ParameterizedType && ((Class) ((ParameterizedType) i).getRawType()).getName().equals(ModularEntryPoint.class.getName())))
                        .forEach(i -> {
                            ParameterizedType parameterizedType = (ParameterizedType) i;
                            //  Extract the actual generic argument type parameters
                            Type[] typeArguments = parameterizedType.getActualTypeArguments();
                            try {
                                Object param = (paramStr != null) ? objectMapper.readValue(paramStr, objectMapper.constructType(typeArguments[0])) : null;
                                Object entryPointResult = targetEntryPointObject.getClass().getMethod("run", Object.class).invoke(targetEntryPointObject, param);
                                result.set(new EntryPointResultWrapper(modularEntryPoint, entryPointResult));
                            } catch (JsonProcessingException | InvocationTargetException |
                                     IllegalAccessException | NoSuchMethodException e) {
                                Throwable cause = e;
                                if (e instanceof InvocationTargetException) {
                                    cause = ((InvocationTargetException) e).getTargetException();
                                }
                                throw new ModuleLoadRuntimeException(moduleName, "Failed to load module '" + moduleName + "' with entry point class name: " + entryPointClass, cause);
                            }
                        });
            } else {
                result.set(new EntryPointResultWrapper(modularEntryPoint, null));
            }
            return result.get();
        } else if (foundEntryPoints.size() > 1) {
            throw new ModuleLoadRuntimeException("There are more than one entry point for module: " + moduleName + " with class name: " + entryPointClass);
        } else {
            throw new ModuleLoadRuntimeException("Entry point " + entryPointClass + " not found in module " + moduleName);
        }
    }

    private List<ModularEntryPoint> getModularEntryPoints(String moduleName, String entryPointClass) {
        List<ModularEntryPoint> entryPoints = List.of();
        try {
            entryPoints = Modular.getModularServices(ModularEntryPoint.class, moduleName);
        } catch (Exception e) {
            log.warn("Error when getModularServices for class ModularEntryPoint", e);
        }
        if (entryPoints == null || entryPoints.isEmpty()) {
            throw new ModuleLoadRuntimeException("Entry point " + entryPointClass + " not found in module " + moduleName);
        }
        Predicate<ModularEntryPoint> filteredByClassName = (entryPoint) -> {
            if (entryPoint.getClass().getName().equals(entryPointClass)) {
                return true;
            } else {
                try {
                    if (getTargetFieldFromProxyObject(entryPoint).getClass().getName().equals(entryPointClass)) {
                        return true;
                    }
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    return false;
                }
                return false;
            }
        };
        return entryPoints.stream().filter(filteredByClassName).collect(Collectors.toList());
    }

    private DuplicatedModuleLoadRuntimeException duplicatedModuleException(String moduleName, CompletableFuture<ModuleDetail> moduleDetailCompletableFuture) {
        DuplicatedModuleLoadRuntimeException exception = new DuplicatedModuleLoadRuntimeException(moduleName, "Module '" + moduleName + "' is already loaded");
        moduleDetailCompletableFuture.completeExceptionally(exception);
        notifyModuleReady(moduleName);
        return exception;
    }

    private Object getTargetFieldFromProxyObject(Object proxy) throws NoSuchFieldException, IllegalAccessException {
        Field field = proxy.getClass().getDeclaredField(PROXY_TARGET_FIELD_NAME);
        field.setAccessible(true);
        return field.get(proxy);
    }

    private void finishLoading(String moduleName, CompletableFuture<ModuleDetail> moduleDetailCompletableFuture, ModuleDetail moduleDetail) {
        moduleDetailCompletableFuture.complete(moduleDetail);
        notifyModuleReady(moduleName);
        log.info("Finish loading module '{}'", moduleName);
    }

    @Override
    public ModuleDetail startModuleSync(String moduleName, List<URI> locationUris, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, null, null, packagesToScan, false);
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startModuleSyncWithMainClass(String moduleName, List<URI> locationUris, String mainClass, List<String> packagesToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, null, mainClass, packagesToScan, false);
        return awaitModuleReady(moduleName, cf);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsync(String moduleName, List<URI> locationUris, List<String> packagesToScan) {
        return startModule(moduleName, locationUris, null, null, packagesToScan, false);
    }

    @Override
    public CompletableFuture<ModuleDetail> startModuleAsyncWithMainClass(String moduleName, List<URI> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, null, mainClass, packageToScan, false);
    }

    // Spring

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClassLoop(String moduleName, List<URI> locationUris, String mainClass, List<String> packageToScan) {
        CompletableFuture<ModuleDetail> cf = startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, true);
        return awaitSpringApplicationContextReady(moduleName, cf);
    }

    @Override
    public ModuleDetail startSpringModuleSyncWithMainClass(String moduleName, List<URI> locationUris, String mainClass, List<String> packageToScan) {
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
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClassLoop(String moduleName, List<URI> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, true);
    }

    @Override
    public CompletableFuture<ModuleDetail> startSpringModuleAsyncWithMainClass(String moduleName, List<URI> locationUris, String mainClass, List<String> packageToScan) {
        return startModule(moduleName, locationUris, ExternalContainer.SPRING, mainClass, packageToScan, false);
    }

    @Override
    public boolean unloadModule(String moduleName) {
        synchronized (moduleLoadingLock) {
            if (moduleDetailMap.containsKey(moduleName)) {
                ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
                if (moduleDetail.getModularEntryPoint() != null) {
                    moduleDetail.getModularEntryPoint().onUnloadModule();
                }
                loadedModularServices2.forEach((k, v) -> {
                    Iterator<ModularServiceHolder> iterator = v.iterator();
                    while (iterator.hasNext()) {
                        ModularServiceHolder serviceHolder = iterator.next();
                        if (serviceHolder.getModuleName().equals(moduleName)) {
                            iterator.remove();
                            loadedProxyObjects.entrySet().removeIf(entry -> entry.getKey().service().equals(serviceHolder.getInstance()));
                        }
                    }
                });
                ModularClassLoader classLoader = modulerClassLoaderMap.get(moduleName);
                classLoader.getModuleNames().removeIf(name -> name.equals(moduleName));
                if (classLoader.getModuleNames().isEmpty()) {
                    try (ModularClassLoader c = modulerClassLoaderMap.remove(moduleName)) {
                        log.info("Remove ModularClassLoader {} of module {}.", c.getName(), moduleName);
                    } catch (IOException e) {
                        throw new ModuleLoadRuntimeException("Failed to unload module {}", moduleName, e);
                    }
                }
                log.info(SUCCESSFULLY_UNLOADED_MODULE_LOG, moduleName);
                moduleDetailMap.remove(moduleName);
            } else {
                log.warn("Module '{}' is not loaded", moduleName);
            }
            return true;
        }
    }

    @Override
    public String getCurrentModuleName() {
        return currentModuleNameThreadLocal.get();
    }

    private ModuleDetail awaitModuleReady(String moduleName, CompletableFuture<ModuleDetail> cf) {
        ModuleDetail moduleDetail = moduleDetailMap.get(moduleName);
        try {
            moduleDetail.getReadyLatch().await();
        } catch (InterruptedException e) {
            throw new ModuleLoadRuntimeException(String.format("Interrupted while waiting for module %s ready", moduleName), e);
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
        if (moduleDetail != null) {
            CountDownLatch readyLatch = moduleDetail.getReadyLatch();
            if (readyLatch != null && readyLatch.getCount() > 0) {
                readyLatch.countDown();
            }
            moduleDetail.setLoadStatus(LoadStatus.LOADED);
        } else {
            throw new ModuleLoadRuntimeException("Module " + moduleName + " not found");
        }
    }

    @Override
    public ModuleDetail getCurrentModuleDetail() {
        return getModuleDetail(this.getCurrentModuleName());
    }

    @Override
    public ModuleDetail getModuleDetail(String moduleName) {
        return this.moduleDetailMap.get(moduleName);
    }
}

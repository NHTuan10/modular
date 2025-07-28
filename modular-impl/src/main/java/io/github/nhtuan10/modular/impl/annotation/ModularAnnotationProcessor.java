package io.github.nhtuan10.modular.impl.annotation;

import io.github.classgraph.*;
import io.github.nhtuan10.modular.api.annotation.ModularConfiguration;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.api.annotation.ModularSpringService;
import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.api.exception.AnnotationProcessingRuntimeException;
import io.github.nhtuan10.modular.api.module.ExternalContainer;
import io.github.nhtuan10.modular.api.module.ModuleLoadConfiguration;
import io.github.nhtuan10.modular.impl.model.ModularServiceHolder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This class is not a thread safe
 */
@NoArgsConstructor
@Slf4j
public class ModularAnnotationProcessor {

    ModularClassLoader modularClassLoader;
    Map<Class<?>, Collection<ModularServiceHolder>> container;

    public ModularAnnotationProcessor(ModularClassLoader modularClassLoader) {
        this.modularClassLoader = modularClassLoader;
        this.container = new ConcurrentHashMap<>();
    }

//    public Map<Class<?>, Collection<ModularServiceHolder>> annotationProcess(String moduleName, List<String> packages, ExternalContainer externalContainer) {
//        if (externalContainer == ExternalContainer.SPRING) {
//            annotationScan(moduleName, packages, ModularConfiguration.class.getName(), ModularSpringService.class.getName(), externalContainer);
//        }
//        annotationScan(moduleName, packages, ModularConfiguration.class.getName(), ModularService.class.getName(), externalContainer);
//        return container;
//    }


    public Map<Class<?>, Collection<ModularServiceHolder>> annotationProcess(String moduleName, ModuleLoadConfiguration moduleLoadConfiguration) {
        List<String> packages = moduleLoadConfiguration.packagesToScan();
        if (packages != null && !packages.isEmpty()) {
            ClassGraph classgraph = new ClassGraph()
                    .overrideClasspath((Object[]) this.modularClassLoader.getURLs())
                    .acceptModules("*")
                    .overrideClassLoaders(this.modularClassLoader)
//                             .verbose()               // may need to use some config to enable verbose to log to stderr
                    .enableAllInfo()         // Scan classes, methods, fields, annotations
                    .acceptPackages(packages.toArray(new String[0]));     // Scan package and subpackages (omit to scan all packages)
            ClassLoader classLoader = this.modularClassLoader;
            String jpmsModuleName = moduleLoadConfiguration.jpmsModuleName();
            if (jpmsModuleName != null) {
                ModuleLayer moduleLayer = modularClassLoader.getModuleLayer(moduleName, jpmsModuleName);
                classgraph = classgraph.overrideModuleLayers(moduleLayer);
                classLoader = moduleLayer.findLoader(jpmsModuleName);
            }
            try (ScanResult scanResult = classgraph.scan()) {               // Start the scan
                processServiceAnnotation(moduleName, AnnotationProcessorConfig.DEFAULT, classLoader, scanResult);
                if (moduleLoadConfiguration.externalContainer() == ExternalContainer.SPRING) {
                    processServiceAnnotation(moduleName, AnnotationProcessorConfig.SPRING, classLoader, scanResult);
                }

                processConfigurationAnnotation(moduleName, AnnotationProcessorConfig.DEFAULT, classLoader, moduleLoadConfiguration.allowNonAnnotatedServices(), scanResult);
                if (moduleLoadConfiguration.externalContainer() == ExternalContainer.SPRING) {
                    processConfigurationAnnotation(moduleName, AnnotationProcessorConfig.SPRING, classLoader, moduleLoadConfiguration.allowNonAnnotatedServices(), scanResult);
                }

            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException | ClassNotFoundException e) {
                throw new AnnotationProcessingRuntimeException(String.format("Failed to process annotation in Modular module %s", moduleName), e);
            }
        }
        return container;
    }

//    void annotationScan(String moduleName, List<String> packages, String configurationAnnotation, String serviceAnnotation, ExternalContainer externalContainer) {
//
//    }

    private void processServiceAnnotation(String moduleName, AnnotationProcessorConfig config, ClassLoader classLoader, ScanResult scanResult) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException, ClassNotFoundException {
        String serviceInterfaceAnnotationName = config.getServiceInterfaceAnnotation().getName();
        String serviceImplAnnotationName = config.getServiceImplAnnotation().getName();
        ExternalContainer externalContainer = config.getExternalContainer();
        ClassInfoList serviceClasses = scanResult.getClassesWithAnnotation(serviceInterfaceAnnotationName);
        for (ClassInfo classInfo : serviceClasses) {
            if (classInfo.isInterface()) {
                Class<?> interfaceClass = classLoader.loadClass(classInfo.getName());
                List<ClassInfo> implClassesInfo = new ArrayList<>(scanResult.getClassesImplementing(interfaceClass.getName()));
                Set<ModularServiceHolder> serviceInfoSet = new LinkedHashSet<>();
                for (ClassInfo implClassInfo : implClassesInfo) {
                    if (implClassInfo.hasAnnotation(serviceImplAnnotationName)) {
                        Class<?> implClass = classLoader.loadClass(implClassInfo.getName());
                        Set<Class<?>> interfaceClasses = new HashSet<>(Set.of(interfaceClass));
                        // check if any service instance of the implementation class exists
                        boolean doesServiceExist = false;
                        for (ClassInfo i : implClassInfo.getInterfaces().filter(c -> !c.equals(classInfo) && c.hasAnnotation(serviceInterfaceAnnotationName))) {
                            Class<?> c = classLoader.loadClass(i.getName());
                            if (container.containsKey(c)) {
                                ModularServiceHolder serviceHolder = container.get(c).stream()
                                        .filter(s -> s.getServiceClass() != null && s.getServiceClass()
                                                .equals(implClass)).findFirst().orElse(null);
                                if (serviceHolder != null) {
                                    doesServiceExist = true;
                                    serviceHolder.getInterfaceClasses().addAll(interfaceClasses);
                                    serviceInfoSet.add(serviceHolder);
                                    break;
                                }
                            }
                        }
                        if (!doesServiceExist) {
                            String name = buildServiceName(moduleName, implClass.getName(), externalContainer);
                            ModularServiceHolder modularServiceHolder;
                            if (externalContainer == null) {
                                Object service = implClass.getConstructor().newInstance();
                                modularServiceHolder = new ModularServiceHolder(moduleName, implClass, name, service, interfaceClasses, modularClassLoader);
                            } else {
                                modularServiceHolder = new ModularServiceHolder(moduleName, implClass, name, interfaceClasses, externalContainer, modularClassLoader);
                                String extBeanName = StringUtils.uncapitalize(implClass.getSimpleName());
                                modularServiceHolder.setExternalBeanName(getServiceExternalBeanName(implClassInfo.getAnnotationInfo(serviceImplAnnotationName), extBeanName));
                            }
                            serviceInfoSet.add(modularServiceHolder);
                        }
                    }
                }
                container.putIfAbsent(interfaceClass, new LinkedHashSet<>());
                container.get(interfaceClass).addAll(serviceInfoSet);
            }
        }
    }

    private String getServiceExternalBeanName(AnnotationInfo annotationInfo, String defaultExternalBeanName) {
        Object extBeanNameParam = annotationInfo.getParameterValues().getValue("beanName");
        if (extBeanNameParam == null || StringUtils.isBlank(extBeanNameParam.toString())) {
            return defaultExternalBeanName;
        } else {
            return extBeanNameParam.toString().trim();
        }
    }

    private String buildServiceName(String moduleName, String className, ExternalContainer externalContainer) {
        return buildServiceName(moduleName, className, null, externalContainer);
    }

    private String buildServiceName(String moduleName, String className, String methodName, ExternalContainer externalContainer) {
        StringBuilder sb = new StringBuilder();
        sb.append(moduleName).append("#").append(className);
        if (methodName != null) {
            sb.append("#").append(methodName);
        }
//        if (externalContainer ) {
//            sb.append("#").append(RandomStringUtils.randomAlphanumeric(10));
//        } else {
//            if (methodName != null) {
//                sb.append("#").append(methodName);
//            }
//        }
        return sb.toString();
    }

    private void processConfigurationAnnotation(String moduleName, AnnotationProcessorConfig config, ClassLoader classLoader, boolean allowNonAnnotatedServices, ScanResult scanResult) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException, ClassNotFoundException {
        String configAnnotation = config.getConfigAnnotation().getName();
        String serviceImplAnnotation = config.getServiceImplAnnotation().getName();
        String serviceInterfaceAnnotation = config.getServiceInterfaceAnnotation().getName();
        ExternalContainer externalContainer = config.getExternalContainer();
        for (ClassInfo configClassInfo : scanResult.getClassesWithAnnotation(configAnnotation)) {
            MethodInfoList methodInfosList = configClassInfo.getMethodInfo().filter(methodInfo -> methodInfo.hasAnnotation(serviceImplAnnotation) && methodInfo.getTypeDescriptor() != null);
            for (MethodInfo methodInfo : methodInfosList) {
                ClassRefTypeSignature classRefTypeSignature = ((ClassRefTypeSignature) methodInfo.getTypeDescriptor().getResultType());
                ClassInfo returnTypeClassInfo = classRefTypeSignature.getClassInfo();
                Class<?> returnTypeClass = classLoader.loadClass(returnTypeClassInfo.getName());
                Set<Class<?>> interfaces = new HashSet<>();
                if (returnTypeClassInfo.isInterface() && (allowNonAnnotatedServices || returnTypeClassInfo.hasAnnotation(serviceInterfaceAnnotation))) {
                    interfaces.add(returnTypeClass);
                }
                Stream<ClassInfo> interfaceStreams = returnTypeClassInfo.getInterfaces().stream();
                if (!allowNonAnnotatedServices) {
                    interfaceStreams = interfaceStreams.filter(interfaceFilter -> interfaceFilter.hasAnnotation(serviceInterfaceAnnotation));
                }
                for (ClassInfo interfaceInfo : interfaceStreams.collect(Collectors.toSet())) {
                    interfaces.add(classLoader.loadClass(interfaceInfo.getName()));
                }
                if (!interfaces.isEmpty()) {
                    Class<?> configClass = classLoader.loadClass(configClassInfo.getName());
                    Method method = configClass.getDeclaredMethod(methodInfo.getName());
                    ModularServiceHolder modularServiceHolder;
                    if (externalContainer == null) {
                        method.setAccessible(true);
                        Constructor<?> constructor = configClass.getDeclaredConstructor();
                        constructor.setAccessible(true);
                        Object object = method.invoke(constructor.newInstance());
                        if (object == null) {
                            throw new AnnotationProcessingRuntimeException(moduleName, String.format("Error processing module %s: Modular service creation method %s#%s returns null, which is not allowed", moduleName, configClassInfo.getName(), method.getName()));
                        }
                        Class<?> serviceClass = object.getClass();
                        modularServiceHolder = new ModularServiceHolder(moduleName, serviceClass, buildServiceName(moduleName, serviceClass.getName(), method.getName(), null), object, interfaces, modularClassLoader);
                    } else {
//                        if (returnTypeClassInfo.isStandardClass()) {
//                            modularServiceHolder.setServiceClass(returnTypeClass);
//                        }
                        modularServiceHolder = new ModularServiceHolder(moduleName, returnTypeClass, buildServiceName(moduleName, configClass.getName(), method.getName(), externalContainer), interfaces, externalContainer, modularClassLoader);
                        modularServiceHolder.setExternalBeanName(getServiceExternalBeanName(methodInfo.getAnnotationInfo(serviceImplAnnotation), method.getName()));
                    }
                    interfaces.forEach(interfaceClass -> {
                        container.putIfAbsent(interfaceClass, new LinkedHashSet<>());
                        container.get(interfaceClass).add(modularServiceHolder);
                    });
                }
            }
        }
    }

    @RequiredArgsConstructor
    @Getter
    public static enum AnnotationProcessorConfig {
        DEFAULT(ModularService.class, ModularService.class, ModularConfiguration.class, null),
        SPRING(ModularService.class, ModularSpringService.class, ModularConfiguration.class, ExternalContainer.SPRING);

        final Class<? extends Annotation> serviceInterfaceAnnotation;
        final Class<? extends Annotation> serviceImplAnnotation;
        final Class<? extends Annotation> configAnnotation;
        final ExternalContainer externalContainer;

    }
}
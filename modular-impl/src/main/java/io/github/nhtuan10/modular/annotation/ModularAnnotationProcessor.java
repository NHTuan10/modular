package io.github.nhtuan10.modular.annotation;

import io.github.classgraph.*;
import io.github.nhtuan10.modular.api.annotation.ModularConfiguration;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.model.ModularServiceHolder;
import io.github.nhtuan10.modular.module.ModularClassLoader;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

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

    public Map<Class<?>, Collection<ModularServiceHolder>> annotationProcess(String moduleName, List<String> packages, boolean lazyInit) {
        annotationScan(moduleName, packages, ModularConfiguration.class.getName(), ModularService.class.getName(), lazyInit);
        return container;
    }

    void annotationScan(String moduleName, List<String> packages, String configurationAnnotation, String serviceAnnotation, boolean lazyInit) {
        if (packages != null && !packages.isEmpty() && StringUtils.isNotBlank(configurationAnnotation) && StringUtils.isNotBlank(serviceAnnotation)) {
            try (ScanResult scanResult =
                         new ClassGraph()
                                 .overrideClasspath(this.modularClassLoader.getClassPathUrls())
                                 .overrideClassLoaders(this.modularClassLoader)
//                             .verbose()               // may need to use some config to enable verbose to log to stderr
                                 .enableAllInfo()         // Scan classes, methods, fields, annotations
                                 .acceptPackages(packages.toArray(new String[0]))     // Scan package and subpackages (omit to scan all packages)
                                 .scan()) {               // Start the scan

                processServiceAnnotation(moduleName, serviceAnnotation, lazyInit, scanResult);

                processConfigurationAnnotation(moduleName, configurationAnnotation, scanResult, serviceAnnotation);

            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processServiceAnnotation(String moduleName, String serviceAnnotation, boolean lazyInit, ScanResult scanResult) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(serviceAnnotation)) {
            if (classInfo.isInterface()) {
                Class<?> interfaceClass = classInfo.loadClass();
                List<ClassInfo> implClassesInfo = scanResult.getClassesImplementing(interfaceClass.getName()).stream()
                        .toList();
                Set<ModularServiceHolder> serviceInfoSet = new HashSet<>();
                for (ClassInfo implClassInfo : implClassesInfo) {
                    if (implClassInfo.hasAnnotation(serviceAnnotation)) {
                        Class<?> implClass = implClassInfo.loadClass();
                        Object service = null;
                        // check if any service instance of the implementation class exists
                        for (ClassInfo i : implClassInfo.getInterfaces().filter(c -> c.hasAnnotation(serviceAnnotation))) {
                            Class<?> c = i.loadClass();
                            if (container.containsKey(c)) {
                                service = container.get(c).stream()
                                        .map(ModularServiceHolder::getInstance).filter(s -> s.getClass().equals(implClass)).findFirst().orElse(null);
                            }
                        }
                        if (service == null && !lazyInit) {
                            service = implClass.getConstructor().newInstance();
                        }

                        serviceInfoSet.add(new ModularServiceHolder(implClass, buildServiceName(moduleName, implClass.getName()), service, interfaceClass));
                    }
                }
                container.putIfAbsent(interfaceClass, new HashSet<>());
                container.get(interfaceClass).addAll(serviceInfoSet);
            }
        }
    }

    private String buildServiceName(String moduleName, String className) {
        return buildServiceName(moduleName, className, null);
    }

    private String buildServiceName(String moduleName, String className, String methodName) {
        if (methodName != null) {
            return moduleName + "#" + className + "#" + methodName;
        } else {
            return moduleName + "#" + className;
        }
    }

    private void processConfigurationAnnotation(String moduleName, String configAnnotation, ScanResult scanResult, String serviceAnnotation) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        for (ClassInfo configClassInfo : scanResult.getClassesWithAnnotation(configAnnotation)) {
            MethodInfoList methodInfosList = configClassInfo.getMethodInfo().filter(methodFilter -> methodFilter.hasAnnotation(serviceAnnotation));
            for (MethodInfo methodInfo : methodInfosList.filter(m -> m.getTypeDescriptor() != null)) {
                ClassRefTypeSignature classRefTypeSignature = ((ClassRefTypeSignature) methodInfo.getTypeDescriptor().getResultType());

                Set<Class<?>> interfaces = new HashSet<>();
                ClassInfo returnTypeClassInfo = classRefTypeSignature.getClassInfo();
                if (returnTypeClassInfo.isInterface() && returnTypeClassInfo.hasAnnotation(serviceAnnotation)) {
                    interfaces.add(returnTypeClassInfo.loadClass());
                }
                interfaces.addAll(returnTypeClassInfo.getInterfaces().stream()
                        .filter(interfaceFilter -> interfaceFilter.hasAnnotation(serviceAnnotation))
                        .map(ClassInfo::loadClass).collect(Collectors.toSet()));
                Class<?> configClass = configClassInfo.loadClass();
                Method method = configClass.getDeclaredMethod(methodInfo.getName());
                method.setAccessible(true);
                var constructor = configClass.getDeclaredConstructor();
                constructor.setAccessible(true);
                Object object = method.invoke(constructor.newInstance());
                Class<?> serviceClass = object.getClass();
                interfaces.forEach(interfaceClass -> {
                    container.putIfAbsent(interfaceClass, new HashSet<>());
                    container.get(interfaceClass).add(new ModularServiceHolder(serviceClass, buildServiceName(moduleName, serviceClass.getName(), method.getName()), object, interfaceClass));
                });

            }
        }
    }
}
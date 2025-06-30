package io.github.nhtuan10.modular.annotation;

import io.github.classgraph.*;
import io.github.nhtuan10.modular.api.annotation.ModularConfiguration;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.api.exception.ProxyCreationException;
import io.github.nhtuan10.modular.model.ModularServiceHolder;
import io.github.nhtuan10.modular.module.ModularClassLoader;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

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

    public Map<Class<?>, Collection<ModularServiceHolder>> getModularServices() {
        return container;
    }

    public Collection<ModularServiceHolder> getModularServices(Class<?> key) {
        return container.get(key);
    }

    public void annotationProcess(List<String> packages, boolean lazyInit) throws ProxyCreationException {
        annotationScan(packages, ModularConfiguration.class.getName(), ModularService.class.getName(), lazyInit);
    }

    void annotationScan(List<String> packages, String configurationAnnotation, String serviceAnnotation, boolean lazyInit) throws ProxyCreationException {
        // TODO: need to handle multiple interfaces too
        if (packages != null && !packages.isEmpty() && StringUtils.isNotBlank(configurationAnnotation) && StringUtils.isNotBlank(serviceAnnotation)) {
            try (ScanResult scanResult =
                         new ClassGraph()
//                             .addClassLoader(this.classLoader)
                                 .overrideClasspath(this.modularClassLoader.getClassPathUrls())
                                 .overrideClassLoaders(this.modularClassLoader)
//                             .verbose()               // Log to stderr
                                 .enableAllInfo()         // Scan classes, methods, fields, annotations
                                 .acceptPackages(packages.toArray(new String[0]))     // Scan package and subpackages (omit to scan all packages)
                                 .scan()) {               // Start the scan

                processServiceAnnotation(serviceAnnotation, lazyInit, scanResult);

                processConfigurationAnnotation(configurationAnnotation, scanResult);

            } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                     NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void processServiceAnnotation(String serviceAnnotation, boolean lazyInit, ScanResult scanResult) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(serviceAnnotation)) {
            if (classInfo.isInterface()) {
                Class<?> interfaceClass = classInfo.loadClass();
                List<ClassInfo> implClassesInfo = scanResult.getClassesImplementing(interfaceClass.getName()).stream()
                        .toList();
                Set<ModularServiceHolder> serviceInfoSet = new HashSet<>();
                for (ClassInfo implClassInfo : implClassesInfo) {
                    if (implClassInfo.hasAnnotation(ModularService.class.getName())) {
                        Class<?> implClass = implClassInfo.loadClass();
                        Object service = null;
                        if (!lazyInit) {
                            service = implClass.getConstructor().newInstance();
                        }

                        serviceInfoSet.add(new ModularServiceHolder(implClass, implClass.getName(), service, interfaceClass));
                    }
                }
                container.put(interfaceClass, serviceInfoSet);
            }
        }
    }

    private void processConfigurationAnnotation(String configAnnotation, ScanResult scanResult) throws IllegalAccessException, InvocationTargetException, NoSuchMethodException, InstantiationException {
        for (ClassInfo configClassInfo : scanResult.getClassesWithAnnotation(configAnnotation)) {
            MethodInfoList methodInfosList = configClassInfo.getMethodInfo().filter(methodFilter -> methodFilter.hasAnnotation(ModularService.class.getName()));
            for (MethodInfo methodInfo : methodInfosList) {
                if (methodInfo.getTypeDescriptor() != null) {
                    ClassRefTypeSignature classRefTypeSignature = ((ClassRefTypeSignature) methodInfo.getTypeDescriptor().getResultType());
                    Class<?> serviceClass = classRefTypeSignature.loadClass();
                    ClassInfoList interfacesClassInfoList = classRefTypeSignature.getClassInfo().getInterfaces();
                    for (ClassInfo interfaceClassInfo : interfacesClassInfoList) {
                        if (interfaceClassInfo.hasAnnotation(ModularService.class.getName())) {
                            Class<?> configClass = configClassInfo.loadClass();
                            Method method = configClass.getDeclaredMethod(methodInfo.getName());
                            method.setAccessible(true);
                            Object object = method.invoke(configClass.getDeclaredConstructor().newInstance());
                            Class<?> interfaceClass = interfaceClassInfo.loadClass();
                            if (!container.containsKey(interfaceClass)) {
                                container.put(interfaceClass, new HashSet<>());
                            }
                            container.get(interfaceClass).add(new ModularServiceHolder(serviceClass, serviceClass.getName(), object, interfaceClass));
                        }
                    }

                }
            }
        }
    }
}
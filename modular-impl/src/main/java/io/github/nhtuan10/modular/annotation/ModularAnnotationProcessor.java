package io.github.nhtuan10.modular.annotation;

import io.github.nhtuan10.modular.api.annotation.ModularConfiguration;
import io.github.nhtuan10.modular.api.annotation.ModularMethod;
import io.github.nhtuan10.modular.api.annotation.ModularService;
import io.github.nhtuan10.modular.classloader.ModularClassLoader;
import io.github.nhtuan10.modular.exception.ProxyCreationException;
import io.github.nhtuan10.modular.model.ModularServiceHolder;
import io.github.classgraph.*;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
        this.container = new ConcurrentHashMap<Class<?>, Collection<ModularServiceHolder>>();
    }

    public Map<Class<?>, Collection<ModularServiceHolder>> getModularServices() {
        return container;
    }

    public Collection<ModularServiceHolder> getModularServices(Class<?> key) {
        return container.get(key);
    }

    private void implementModularMethod(Object object) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Class<?> clazz = object.getClass();
        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(ModularMethod.class)) {
                method.setAccessible(true);
//                method.invoke(object);
            }
        }
    }

    private void implementModularMethods(Class<?> clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, InstantiationException {

        for (Method method : clazz.getDeclaredMethods()) {
            Object object = clazz.getConstructor().newInstance();
            method.setAccessible(true);
            method.invoke(object);
        }
    }

//    public void configurationAnnotationProcessor(String pkg) {
//        configurationAnnotationProcessor(pkg, ModularConfiguration.class.getName());
//    }

//    void configurationAnnotationProcessor(String pkg, String annotation) {
//        try (ScanResult scanResult =
//                     new ClassGraph()
////                             .addClassLoader(this.classLoader)
//                             .overrideClasspath(this.modularClassLoader.getClassPathUrls())
//                             .overrideClassLoaders(this.modularClassLoader)
////                             .verbose()               // Log to stderr
//                             .enableAllInfo()         // Scan classes, methods, fields, annotations
//                             .acceptPackages(pkg)     // Scan package and subpackages (omit to scan all packages)
//                             .scan()) {               // Start the scan
//
//            for (ClassInfo configClassInfo : scanResult.getClassesWithAnnotation(annotation)) {
//                MethodInfoList methodInfosList = configClassInfo.getMethodInfo().filter(methodFilter -> methodFilter.hasAnnotation(ModularService.class.getName()));
//                for (MethodInfo methodInfo : methodInfosList) {
//                    Set<ModularServiceHolder> serviceInfoSet = new HashSet<>();
//                    if (methodInfo.getTypeDescriptor() != null) {
//                        ClassRefTypeSignature classRefTypeSignature = ((ClassRefTypeSignature) methodInfo.getTypeDescriptor().getResultType());
//                        Class<?> serviceClass = classRefTypeSignature.loadClass();
//                        ClassInfoList interfacesClassInfoList = classRefTypeSignature.getClassInfo().getInterfaces();
//                        for (ClassInfo interfaceClassInfo : interfacesClassInfoList) {
//                            if (interfaceClassInfo.hasAnnotation(ModularService.class.getName())) {
//                                Class<?> configClass = configClassInfo.loadClass();
//                                Object object = configClass.getDeclaredMethod(methodInfo.getName()).invoke(configClass.getDeclaredConstructor().newInstance());

    /// /                                Object object = serviceClass.getDeclaredMethod(methodInfo.getName()).invoke(serviceClass);
//                                Class<?> interfaceClass = interfaceClassInfo.loadClass();
//                                if (!container.containsKey(interfaceClass)) {
//                                    container.put(interfaceClass, new HashSet<>());
//                                }
//                                container.get(interfaceClass).add(new ModularServiceHolder(serviceClass, serviceClass.getName(), object, interfaceClass));
//
//                            }
//                        }
//
//                    }
//                }
//            }
//        } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException |
//                 InstantiationException e) {
//            throw new RuntimeException(e);
//        }
//    }

    public void annotationProcess(String pkg, boolean lazyInit) throws ProxyCreationException {
        annotationScan(pkg, ModularConfiguration.class.getName(), ModularService.class.getName(), lazyInit);
    }

    void annotationScan(String pkg, String configurationAnnotation, String serviceAnnotation, boolean lazyInit) throws ProxyCreationException {
        // TODO: need to handle multiple interfaces too
        try (ScanResult scanResult =
                     new ClassGraph()
//                             .addClassLoader(this.classLoader)
                             .overrideClasspath(this.modularClassLoader.getClassPathUrls())
                             .overrideClassLoaders(this.modularClassLoader)
//                             .verbose()               // Log to stderr
                             .enableAllInfo()         // Scan classes, methods, fields, annotations
                             .acceptPackages(pkg)     // Scan package and subpackages (omit to scan all packages)
                             .scan()) {               // Start the scan

            processServiceAnnotation(serviceAnnotation, lazyInit, scanResult);

            processConfigurationAnnotation(configurationAnnotation, scanResult);

        } catch (InvocationTargetException | InstantiationException | IllegalAccessException |
                 NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private void processServiceAnnotation(String serviceAnnotation, boolean lazyInit, ScanResult scanResult) throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        for (ClassInfo classInfo : scanResult.getClassesWithAnnotation(serviceAnnotation)) {
//                AnnotationInfo annotationInfo = routeClassInfo.getAnnotationInfo(annotation);
            if (classInfo.isInterface()) {
                Class<?> interfaceClass = classInfo.loadClass();
//                    Class<?> interfaceClass = this.modularClassLoader.loadClass(classInfo.getName());
//                    Class<?> interfaceClass = classLoader.loadClass(classInfo.getName());
//                    List<? extends Class<?>> implClasses = scanResult.getClassesImplementing(interfaceClass.getName()).stream()
                List<ClassInfo> implClassesInfo = scanResult.getClassesImplementing(interfaceClass.getName()).stream()
//                            .map(c -> {
//                                try {
//                                    return Class.forName(c.getName(), true, modularClassLoader);
//                                } catch (ClassNotFoundException e) {
//                                    throw new RuntimeException("Internal error when loading class " + c.getName() , e);
//                                }
//                            })
//                            .map(ClassInfo::loadClass)
                        .toList();
//                    implClasses.map(ClassInfo::loadClass);
                Set<ModularServiceHolder> serviceInfoSet = new HashSet<>();
                for (ClassInfo implClassInfo : implClassesInfo) {
//                        try {
//                            serviceInfoSet.add(new ModularServiceHolder(implClass, implClass.getName(), ProxyCreator.createNoArgsContructorsProxyClass(interfaceClass, implClass), interfaceClass));
                    if (implClassInfo.hasAnnotation(ModularService.class.getName())) {
                        Class<?> implClass = implClassInfo.loadClass();
                        Object service = null;
                        if (!lazyInit) {
                            service = implClass.getConstructor().newInstance();
                        }

                        serviceInfoSet.add(new ModularServiceHolder(implClass, implClass.getName(), service, interfaceClass));
                    }

//                        } catch (Exception e) {
//                            throw new ProxyCreationException("Failed to create proxy for class %s with annotation %s in package %s".formatted(implClass.getName(), annotation, pkg), e);
//                        }

                }

//                    container.put(interfaceClass, Collections.unmodifiableSet(serviceInfoSet));
                container.put(interfaceClass, serviceInfoSet);
//                    for (Class<?> implClass : implClasses) {
////                        implementModularMethods(implClass);
//                        if (!container.containsKey(interfaceClass)) {
//                            container.put(interfaceClass, new ArrayList<>());
//                        }
//                        container.get(interfaceClass).add(createProxyClass(interfaceClass, implClass));
//                    }
            }

            ;
//                List<AnnotationParameterValue> routeParamVals = routeAnnotationInfo.getParameterValues();
            // @com.xyz.Route has one required parameter
//                String route = (String) routeParamVals.get(0).getValue();
//                System.out.println(routeClassInfo.getName() + " is annotated with route " + route);
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
                            Object object = configClass.getDeclaredMethod(methodInfo.getName()).invoke(configClass.getDeclaredConstructor().newInstance());
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

//    public void annotationProcessUsingSpring() {
//        ClassPathScanningCandidateComponentProvider scanner =
//                new ClassPathScanningCandidateComponentProvider(false);
//
//        scanner.addIncludeFilter(new AnnotationTypeFilter(ModularService.class));
//
//        for (BeanDefinition bd : scanner.findCandidateComponents("com.example")) {
////            try {
//            System.out.println(bd.getBeanClassName());
//
////                implementModularMethods(Class.forName(bd.getBeanClassName()));
////            } catch (IllegalAccessException | InvocationTargetException | ClassNotFoundException |
////                     NoSuchMethodException | InstantiationException e) {
////                throw new RuntimeException(e);
////            }
//        }
//    }

}
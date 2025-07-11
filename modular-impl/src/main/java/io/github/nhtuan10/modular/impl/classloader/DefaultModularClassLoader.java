package io.github.nhtuan10.modular.impl.classloader;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DefaultModularClassLoader extends URLClassLoader implements ModularClassLoader {

    public static final String MODULAR_PARENT_PACKAGE = "io.github.nhtuan10.modular";

    public static final Set<String> MODULAR_PACKAGES = Set.of(MODULAR_PARENT_PACKAGE + ".api"
            , MODULAR_PARENT_PACKAGE + ".impl.annotation"
            , MODULAR_PARENT_PACKAGE + ".impl.classloader"
            , MODULAR_PARENT_PACKAGE + ".impl.model"
            , MODULAR_PARENT_PACKAGE + ".impl.module"
//            , MODULAR_PARENT_PACKAGE + ".impl.proxy"
            , MODULAR_PARENT_PACKAGE + ".impl.serdeserializer"
    );

    @Getter
    private Set<String> excludedClassPackages;

    @Getter
    private List<URL> classPathUrls;

    @Getter
    private final String moduleName;

    public DefaultModularClassLoader(String moduleName, List<URL> classPathUrls, Set<String> excludedClassPackages) {
        this(moduleName, classPathUrls);
        this.excludedClassPackages = Stream.concat(excludedClassPackages.stream(), this.getDefaultExcludedPackages().stream()).collect(Collectors.toUnmodifiableSet());
    }

    public DefaultModularClassLoader(String moduleName, List<URL> classPathUrls) {
        this(moduleName);
        this.classPathUrls = Stream.concat(classPathUrls.stream(), this.classPathUrls.stream()).toList();
        classPathUrls.forEach(this::addURL);
    }

    public DefaultModularClassLoader(String moduleName) {
        super(Collections.unmodifiableList(getJavaClassPath()).toArray(new URL[0]));
//        super(moduleName, getSystemClassLoader());
        this.moduleName = moduleName;
        this.excludedClassPackages = Collections.unmodifiableSet(getDefaultExcludedPackages());
        this.classPathUrls = Collections.unmodifiableList(getJavaClassPath());
    }

    @Override
    public String getName() {
        return this.getClass().getName() + "[" + moduleName + "]";
    }

    protected Set<String> getDefaultExcludedPackages() {
//        return ModuleLayer.boot().modules().stream()
//                .map(Module::getName)
//                .collect(Collectors.toSet());
        return new HashSet<>();
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        log.trace("Loading class: ", name);
        synchronized (getClassLoadingLock(name)) {
            // check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
            if (c == null) {
                try {
                    try {
                        if (shouldLoadBySystemClassLoader(name)) {
                            c = ClassLoader.getSystemClassLoader().loadClass(name);
                        }
                    } catch (ClassNotFoundException e) {
                        // add logs if later needed
                    }
                    if (c == null) {
                        c = findClass(name);
                    }
                } catch (ClassNotFoundException | SecurityException e) {
                    // add logs if later needed
                }

                if (c == null) {
                    // If still not found, then invoke System (a.k.a app) class loader
                    c = super.loadClass(name, false);
                }
            }
            if (resolve) {
                resolveClass(c);
            }
            return c;
        }
    }

    protected boolean shouldLoadBySystemClassLoader(String name) {
        Set<String> platformClassLoaderPackages = new HashSet<>(Set.of("java", "jdk"));
        platformClassLoaderPackages.addAll(MODULAR_PACKAGES);
        platformClassLoaderPackages.addAll(excludedClassPackages);
        return platformClassLoaderPackages.stream().anyMatch(name::startsWith);
    }

    private static List<URL> getJavaClassPath() {
        String classPath = System.getProperty("java.class.path");
        return Arrays.stream(classPath.split(File.pathSeparator))
                .map(path -> {
                    try {
                        return new File(path).toURI().toURL();
                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }
}
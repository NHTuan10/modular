package io.github.nhtuan10.modular.impl.classloader;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DefaultModularClassLoader extends ModularClassLoader {

    public static final String MODULAR_PARENT_PACKAGE = "io.github.nhtuan10.modular";

    public static final Set<String> MODULAR_PACKAGES = Set.of(MODULAR_PARENT_PACKAGE + ".api"
            , MODULAR_PARENT_PACKAGE + ".impl.annotation"
            , MODULAR_PARENT_PACKAGE + ".impl.classloader"
            , MODULAR_PARENT_PACKAGE + ".impl.model"
            , MODULAR_PARENT_PACKAGE + ".impl.module"
            , MODULAR_PARENT_PACKAGE + ".impl.proxy"
            , MODULAR_PARENT_PACKAGE + ".impl.serdeserializer"
    );

    @Getter
    private final String name;

    @Getter
    private final boolean doesIncludeSystemClasspath;

    private DefaultModularClassLoader(String name, List<String> moduleNames, List<URL> classPathUrls, ClassLoader parentClassLoader, Set<String> prefixesLoadedBySystemClassLoader, boolean doesIncludeSystemClasspath) {
//        super(Collections.unmodifiableList(getJavaClassPath()).toArray(new URL[0]));
        super(new URL[0], parentClassLoader, moduleNames);
        this.name = name;
        this.doesIncludeSystemClasspath = doesIncludeSystemClasspath;
        addPrefixesLoadedBySystemClassLoader(getDefaultExcludedPackages());
        if (prefixesLoadedBySystemClassLoader != null) {
            addPrefixesLoadedBySystemClassLoader(prefixesLoadedBySystemClassLoader);
        }
        addClassPathUrls(classPathUrls);
        if (doesIncludeSystemClasspath) {
            addClassPathUrls(getJavaClassPath());
        }
    }


    public DefaultModularClassLoader(List<String> moduleNames, List<URL> classPathUrls, ClassLoader parentClassLoader, Set<String> prefixesLoadedBySystemClassLoader, boolean doesIncludeSystemClasspath) {
        this(moduleNames.get(0), moduleNames, classPathUrls, parentClassLoader, prefixesLoadedBySystemClassLoader, doesIncludeSystemClasspath);
    }

//    public DefaultModularClassLoader(List<String> moduleNames, List<URL> classPathUrls) {
//        this(moduleNames.get(0), moduleNames, classPathUrls, null, Collections.emptySet(), false);
//    }
//
//    public DefaultModularClassLoader(List<String> moduleNames) {
//        this(moduleNames.get(0), moduleNames, Collections.emptyList(), null, Collections.emptySet(), false);
//    }

    public DefaultModularClassLoader(String name, List<String> moduleNames, ClassLoader parentClassLoader, Set<String> prefixesLoadedBySystemClassLoader, boolean doesIncludeSystemClasspath) {
        this(name, moduleNames, Collections.emptyList(), parentClassLoader, prefixesLoadedBySystemClassLoader, doesIncludeSystemClasspath);
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return "DefaultModularClassLoader{" +
                "name='" + name + '\'' +
                ", moduleName='" + moduleNames + '\'' +
                '}';
    }

    protected Set<String> getDefaultExcludedPackages() {
//        return ModuleLayer.boot().modules().stream()
//                .map(Module::getName)
//                .collect(Collectors.toSet());
        Set<String> platformClassLoaderPackages = new HashSet<>(Set.of("java", "jdk"));
        platformClassLoaderPackages.addAll(MODULAR_PACKAGES);
        return platformClassLoaderPackages;
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
        return prefixesLoadedBySystemClassLoader.stream().anyMatch(name::startsWith);
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
                .collect(Collectors.toList());
    }
}
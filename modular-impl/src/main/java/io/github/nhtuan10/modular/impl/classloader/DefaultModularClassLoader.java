package io.github.nhtuan10.modular.impl.classloader;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Getter;
import lombok.Locked;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    private Set<String> prefixesLoadedBySystemClassLoader;

    @Getter
    private List<URL> classPathUrls;

    @Getter
    private final List<String> moduleNames;

    @Getter
    private final String name;

    public DefaultModularClassLoader(String name, List<String> moduleNames, List<URL> classPathUrls, Set<String> prefixesLoadedBySystemClassLoader) {
        super(Collections.unmodifiableList(getJavaClassPath()).toArray(new URL[0]));
        this.moduleNames = Collections.synchronizedList(new ArrayList<>(moduleNames));
        this.name = name;
        this.prefixesLoadedBySystemClassLoader = ConcurrentHashMap.newKeySet();
        addPrefixesLoadedBySystemClassLoader(getDefaultExcludedPackages());
        if (prefixesLoadedBySystemClassLoader != null) {
            addPrefixesLoadedBySystemClassLoader(prefixesLoadedBySystemClassLoader);
        }
        this.classPathUrls = getJavaClassPath();
        addClassPathUrls(classPathUrls);
    }


    public DefaultModularClassLoader(List<String> moduleNames, List<URL> classPathUrls, Set<String> prefixesLoadedBySystemClassLoader) {
        this(moduleNames.get(0), moduleNames, classPathUrls, prefixesLoadedBySystemClassLoader);
    }

    public DefaultModularClassLoader(List<String> moduleNames, List<URL> classPathUrls) {
        this(moduleNames.get(0), moduleNames, classPathUrls, Collections.emptySet());
    }

    public DefaultModularClassLoader(List<String> moduleNames) {
        this(moduleNames.get(0), moduleNames, Collections.emptyList(), Collections.emptySet());
    }

    public DefaultModularClassLoader(String name, List<String> moduleNames, Set<String> prefixesLoadedBySystemClassLoader) {
        this(name, moduleNames, Collections.emptyList(), prefixesLoadedBySystemClassLoader);
    }

    public void addModule(String moduleName) {
        this.moduleNames.add(moduleName);
    }

    @Override
    @Locked.Write
    public void addClassPathUrls(List<URL> classPathUrls) {
        this.classPathUrls = Stream.concat(classPathUrls.stream(), this.classPathUrls.stream()).collect(Collectors.toList());
        classPathUrls.forEach(this::addURL);
    }

    @Override
    public void addPrefixesLoadedBySystemClassLoader(Set<String> prefixesLoadedBySystemClassLoader) {
        this.prefixesLoadedBySystemClassLoader.addAll(prefixesLoadedBySystemClassLoader);
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
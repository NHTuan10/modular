package io.github.nhtuan10.modular.impl.classloader;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Getter;
import lombok.Locked;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.io.File;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class DefaultModularClassLoader extends ModularClassLoader {

    public static final String MODULAR_PARENT_PACKAGE = "io.github.nhtuan10.modular";

    public static final String MODULAR_IMPL_PACKAGE = MODULAR_PARENT_PACKAGE + ".impl";

    public static final Set<String> MODULAR_PACKAGES = Set.of(
            MODULAR_PARENT_PACKAGE + ".api"
            , MODULAR_PARENT_PACKAGE + ".context"
            , MODULAR_IMPL_PACKAGE + ".context"
            , MODULAR_IMPL_PACKAGE + ".annotation"
            , MODULAR_IMPL_PACKAGE + ".classloader"
            , MODULAR_IMPL_PACKAGE + ".model"
            , MODULAR_IMPL_PACKAGE + ".module"
            , MODULAR_IMPL_PACKAGE + ".proxy"
            , MODULAR_IMPL_PACKAGE + ".experimental"
            , MODULAR_IMPL_PACKAGE + ".serdeserializer"
    );

    @Getter
    private Set<String> prefixesLoadedBySystemClassLoader;

    @Getter
    private List<URL> classPathUrls;

    @Getter
    private final List<String> moduleNames;

    @Getter
    private final String name;

    private final Map<Pair<String, String>, ModuleLayer> jpmsModuleLayers = new ConcurrentHashMap<>();

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
    public ModuleLayer getModuleLayer(String moduleName, String jpmsModuleName) {
        return jpmsModuleLayers.get(Pair.of(moduleName, jpmsModuleName));
    }

    @Override
    @Locked.Write
    public void setModuleLayer(String moduleName, String jpmsModuleName) {
//        Path pth = FileSystems.getDefault().getPath("C:\\Users\\archd\\Desktop");
        Path[] paths = classPathUrls.stream().filter(url -> !url.toString().contains("shrinkwrap")).map(url -> {
            try {
                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).toArray(Path[]::new);
        ModuleFinder mf = ModuleFinder.of(paths);
        //Create a new Configuration for a new module layer deriving from the boot configuration, and resolving
        //the JPMS module.
//        Configuration cfg = ModuleLayer.boot().configuration().resolve(mf, ModuleFinder.of(), Set.of(jpmsModuleName, MODULAR_IMPL_PACKAGE));
        Configuration cfg = ModuleLayer.boot().configuration().resolve(mf, ModuleFinder.of(), Set.of(jpmsModuleName));
        Module unnamed = Maven.class.getClassLoader().getUnnamedModule();
        //make the module layer, using the configuration and classloader.
        ModuleLayer ml = ModuleLayer.boot().defineModulesWithOneLoader(cfg, this);
//        Set<String> openUnnamedToModules = Set.of(MODULAR_PARENT_PACKAGE, MODULAR_PARENT_PACKAGE + ".impl");
//        ml.modules().stream().filter(m -> openUnnamedToModules.contains(m.getName())).forEach(module -> {
        // TODO: need to exclude packages having issues when open
//        Optional<Module> modularImplOptional = ml.findModule(MODULAR_IMPL_PACKAGE);
        Module modularImpl = this.getClass().getModule();
        ml.modules().forEach(module -> {
//            final Set<String> packages = unnamed.getPackages();
//            for (String eachPackage : packages) {
//                try {
//                    unnamed.addOpens(eachPackage, module);
//                    log.debug("--add-open " + eachPackage + " from " + unnamed + " to " + module);
//                } catch (Exception e) {
//                    log.debug("Cannot add opens package {} from  un-named module {} to module {}", eachPackage, unnamed, module, e);
//                }
//            }
            if (!module.getName().startsWith(MODULAR_PARENT_PACKAGE)) {

                module.getPackages()
//                        .filter(pkg -> module.isOpen(pkg, modularImpl))
                        .forEach((eachPackage) -> {
                            try {
                                addAllOpens(module, eachPackage, modularImpl);
                                module.addOpens(eachPackage, unnamed);
                                log.debug("--add-open " + eachPackage + " from " + module + " to " + unnamed);
                            } catch (Exception e) {
                                log.debug("Cannot add opens package {} from module {} to module {}", eachPackage, module, unnamed, e);
                            }
                        });
            }
        });
//        ml.findModule(jpmsModuleName).ifPresent(module -> {
//            final Set<String> packages = module.getPackages();
//            for (String eachPackage : packages) {
//                module.addOpens(eachPackage, unnamed);
//            }
//        });
        jpmsModuleLayers.put(Pair.of(moduleName, jpmsModuleName), ml);
    }

    private void addAllOpens(Module fromModule, String eachPackage, Module toModule) throws NoSuchMethodException {
        final Method method = Module.class.getDeclaredMethod("implAddExportsOrOpens", String.class, Module.class, boolean.class, boolean.class);
        method.setAccessible(true);
        try {
            method.invoke(fromModule, eachPackage, toModule, false, true); // add exports
            method.invoke(fromModule, eachPackage, toModule, true, true); // add open
        } catch (Exception e) {
            log.debug("Error when add-opens {}/{}={}", fromModule.getName(), eachPackage, toModule.toString(), e);
        }
        log.info("--add-open " + fromModule.getName() + "/" + eachPackage + "=" + toModule.toString());

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

//    @Override
//    public Class<?> findClass(String moduleName, String name)  {
//        try {
//            return   jpmsModuleLayers.get(moduleName).findLoader(moduleName).loadClass(name);
//        } catch (ClassNotFoundException e) {
//            return null;
//        }
//    }

    @Override
    public Class<?> loadClass(String moduleName, String jpmsModuleName, String className) throws ClassNotFoundException {
        return getModuleLayer(moduleName, jpmsModuleName).findLoader(jpmsModuleName).loadClass(className);
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
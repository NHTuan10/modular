package io.github.nhtuan10.modular.module;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Stream;

@Slf4j
public class ModularClassLoader extends URLClassLoader {
    private final static Logger LOGGER =
            LoggerFactory.getLogger(ModularClassLoader.class.getName());

    public static final String MODULAR_PACKAGE = "io.github.nhtuan10.modular";

    @Getter
    private Set<String> excludedClassPackages;

    @Getter
    private List<URL> classPathUrls;

    @Getter
    private String moduleName;

    URLClassLoader urlClassLoader;

    public void setExcludedClassPackages(Set<String> excludedClassPackages) {
        this.excludedClassPackages = Collections.unmodifiableSet(excludedClassPackages);
    }

    public ModularClassLoader(String moduleName, List<URL> classPathUrls) {
        this(moduleName);
        this.classPathUrls = Stream.concat(classPathUrls.stream(), this.classPathUrls.stream()).toList();
        classPathUrls.forEach(this::addURL);
    }

    public ModularClassLoader(String moduleName) {
        super(Collections.unmodifiableList(getJavaClassPath()).toArray(new URL[0]));
//        super(moduleName, getSystemClassLoader());
        this.moduleName = moduleName;
        this.excludedClassPackages = Collections.unmodifiableSet(getDefaultExcludedPackages());
        this.classPathUrls = Collections.unmodifiableList(getJavaClassPath());
    }
    // add set of string to classPathUrls property
//    public CustomClassLoader addClassPathUrls(List<URL> classPathUrls){
//        this.classPathUrls.addAll(classPathUrls);
//        return this;
//    }

    public URLClassLoader getUrlClassLoader() {
        return new URLClassLoader(this.classPathUrls.toArray(URL[]::new));
    }
//    @Override
//    protected Class findClass(String name) throws ClassNotFoundException {
//        try {
//            loadClassFromFile(name);
//        } catch (MalformedURLException e) {
//            throw new RuntimeException(e);
//        }

    /// /        return defineClass(name, b, 0, b.length);
//    }
    protected Set<String> getDefaultExcludedPackages() {
//        return ModuleLayer.boot().modules().stream()
//                .map(Module::getName)
//                .collect(Collectors.toSet());
        return new HashSet<>();
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
//        LOGGER.trace("Loading class: ", name);
        synchronized (getClassLoadingLock(name)) {
            // check if the class has already been loaded
            Class<?> c = findLoadedClass(name);
//            Class<?> c = null;
            if (c == null) {
                try {
                    try {
                        if (shouldLoadByPlatformClassLoader(name)) {
                            c = ClassLoader.getPlatformClassLoader().loadClass(name);
                        }
                    } catch (ClassNotFoundException e) {
                        // add logs
                    }
                    if (c == null) {
                        c = findClass(name);
                    }
                } catch (ClassNotFoundException | SecurityException e) {
                    // add logs
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

    protected boolean shouldLoadByPlatformClassLoader(String name) {
        return name.startsWith("java") || name.startsWith("jdk") ||
//                || name.startsWith(MODULAR_PACKAGE)
                excludedClassPackages.stream().anyMatch(name::startsWith);
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

//    private Class<?> loadClassFromUrls(String name) throws ClassNotFoundException {
//
//        String path = name.replace('.', File.separatorChar) + ".class";
//        for (URL classPathUrl : classPathUrls) {
////            String file = classPathUrl.toString();
//            String formattedUrlStr = classPathUrl.toString();
//            if (formattedUrlStr.endsWith(".jar")) {
//                formattedUrlStr = "jar:%s!/%s".formatted(formattedUrlStr, path);
//            } else {
//                formattedUrlStr = formattedUrlStr.endsWith("/") ? formattedUrlStr + path : formattedUrlStr + "/" + path;
//            }
//            try (InputStream is = new URL(formattedUrlStr).openConnection().getInputStream();) {
//                byte[] b = is.readAllBytes();
//                return defineClass(null, b, 0, b.length);
//            } catch (IOException e) {
////                log.debug("Error when loading class from {}", formattedUrlStr, e);
//            }
//        }
//        return null;
//    }
//        try (URLClassLoader urlClassLoader = new URLClassLoader(this.classPathUrls.toArray(URL[]::new), getParent());
//             InputStream is = urlClassLoader.getResourceAsStream(path);
//        ){
////            URLClassPath
//            byte[] b = is.readAllBytes();
////            return urlClassLoader.loadClass(name);
//            return  defineClass(name, b, 0, b.length);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

//        InputStream inputStream = getClass().getClassLoader().getResourceAsStream(
//                name.replace('.', File.separatorChar) + ".class");
//        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
//        int nextValue = 0;
//        try {
//            while ((nextValue = inputStream.read()) != -1) {
//                byteStream.write(nextValue);
//            }
//            inputStream.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return byteStream.toByteArray();

//    public static void main(String[] args) throws ClassNotFoundException {
//        new CustomClassLoader().loadClass("com.example.vtweb.ModularMain");
//    }
}
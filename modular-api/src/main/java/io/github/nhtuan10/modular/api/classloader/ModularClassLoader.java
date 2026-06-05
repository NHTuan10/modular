package io.github.nhtuan10.modular.api.classloader;

import lombok.Getter;
import lombok.Locked;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class ModularClassLoader extends URLClassLoader {
    @Getter
    protected final List<String> moduleNames;
    @Getter
    protected List<URL> classPathUrls;
    @Getter
    protected Set<String> prefixesLoadedBySystemClassLoader;

    public ModularClassLoader(URL[] urls, List<String> moduleNames) {
        super(urls, null);
        this.moduleNames = moduleNames;
    }

    public ModularClassLoader(URL[] urls, ClassLoader parent, List<String> moduleNames) {
        super(urls, parent);
        this.moduleNames = Collections.synchronizedList(new ArrayList<>(moduleNames));
        this.classPathUrls = new ArrayList<>();
        this.prefixesLoadedBySystemClassLoader = ConcurrentHashMap.newKeySet();
    }

    public void addModule(String moduleName) {
        moduleNames.add(moduleName);
    }

    @Locked.Write
    public void addClassPathUrls(List<URL> classPathUrls) {
        this.classPathUrls = Stream.concat(this.classPathUrls.stream(), classPathUrls.stream()).collect(Collectors.toList());
        classPathUrls.forEach(this::addURL);
    }

    public String getClassPath() {
        return this.classPathUrls.stream().map(URL::toString).collect(Collectors.joining(File.pathSeparator));
    }

    public void addPrefixesLoadedBySystemClassLoader(Set<String> prefixesLoadedBySystemClassLoader) {
        this.prefixesLoadedBySystemClassLoader.addAll(prefixesLoadedBySystemClassLoader);
    }

}

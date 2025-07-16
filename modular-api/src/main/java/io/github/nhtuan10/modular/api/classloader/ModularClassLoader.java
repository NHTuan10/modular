package io.github.nhtuan10.modular.api.classloader;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Set;

public abstract class ModularClassLoader extends URLClassLoader {
    public ModularClassLoader(URL[] urls) {
        super(urls);
    }

    public abstract void addModule(String moduleName);

    public abstract void addClassPathUrls(List<URL> classPathUrls);

    public abstract void addPrefixesLoadedBySystemClassLoader(Set<String> prefixesLoadedBySystemClassLoader);
}

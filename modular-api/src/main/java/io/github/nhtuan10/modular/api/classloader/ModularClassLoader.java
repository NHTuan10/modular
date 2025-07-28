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

    public abstract List<String> getModuleNames();

    public abstract void setModuleLayer(String moduleName, String jpmsModuleName);

    public abstract ModuleLayer getModuleLayer(String moduleName, String jpmsModuleName);

    public abstract Class<?> loadClass(String moduleName, String jpmsModuleName, String className) throws ClassNotFoundException;
}

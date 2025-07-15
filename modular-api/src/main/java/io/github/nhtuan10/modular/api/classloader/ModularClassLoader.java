package io.github.nhtuan10.modular.api.classloader;

import java.net.URL;
import java.net.URLClassLoader;

public abstract class ModularClassLoader extends URLClassLoader {
    public ModularClassLoader(URL[] urls) {
        super(urls);
    }
}

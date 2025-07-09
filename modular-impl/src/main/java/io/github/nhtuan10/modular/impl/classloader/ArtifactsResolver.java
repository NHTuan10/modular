package io.github.nhtuan10.modular.impl.classloader;

import java.util.List;

public interface ArtifactsResolver<T> {

    List<T> resolveDependencies(List<String> deps, Class<T> clazz);
}

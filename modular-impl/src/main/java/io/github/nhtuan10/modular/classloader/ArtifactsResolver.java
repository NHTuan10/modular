package io.github.nhtuan10.modular.classloader;

import java.util.List;

public interface ArtifactsResolver<T> {

    List<T> resolveMavenDeps(List<String> deps, Class<T> clazz);
}

package io.github.nhtuan10.modular.impl.classloader;

import io.github.nhtuan10.modular.api.classloader.ArtifactsResolver;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.util.List;

@Slf4j
public class MavenArtifactsResolver<T> implements ArtifactsResolver<T> {

    public List<T> resolveDependencies(List<String> deps, Class<T> clazz) {
        List<T> dependencyLocations = Maven.resolver()
                .resolve(deps)
                .withTransitivity().asList(clazz);

        log.debug("Dependency locations for {} : {}", deps, dependencyLocations);
        return dependencyLocations;
    }
}

package io.github.nhtuan10.modular.classloader;

import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.util.List;

public class MavenArtifactsResolver<T> implements ArtifactsResolver<T> {

    public  List<T> resolveMavenDeps(List<String> deps, Class<T> clazz) {
//        Maven.configureResolver().withRemoteRepo("central", "https://repo.maven.apache.org/maven2/", "default")
//                .withMavenCentralRepo(true)
//                .resolve();
        List<T> dependencyLocation = Maven.resolver()
//                .resolve("org.apache.commons:commons-lang3:3.9")
//                .resolve("org.eclipse.aether:aether-transport-http:1.1.0")
                .resolve(deps)
                .withTransitivity().asList(clazz);

        System.out.println(dependencyLocation);
        return dependencyLocation;
    }
}

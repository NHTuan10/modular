package io.github.nhtuan10.sample.launcher;

import io.github.nhtuan10.modular.impl.classloader.DefaultModularClassLoader;
import io.github.nhtuan10.sample.api.service.ServiceException;
import io.github.nhtuan10.sample.plugin1.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;

import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Slf4j
public class JpmsSampleMain {
    public static void main(String[] args) throws ServiceException, ClassNotFoundException {
//        ServiceImpl service = new ServiceImpl();
//        service.baseMethod();
        log.info("Hello World!");
        List<URL> dependencyLocations = Maven.resolver()
//                .resolve("io.github.nhtuan10:modular-sample-api:0.0.1")
                .resolve("io.github.nhtuan10:modular-sample-plugin-1:0.0.1")
                .withTransitivity().asList(URL.class);
        log.info("Dependency locations: " + dependencyLocations);
        ModuleFinder mf = ModuleFinder.of(dependencyLocations.stream().filter(url -> !url.toString().contains("shrinkwrap")).map(url -> {
            try {

                return Paths.get(url.toURI());
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toList()).toArray(new Path[]{}));
        ModuleReference moduleReference = mf.find("io.github.nhtuan10.sample.plugin1").orElse(null);
        //Create a new Configuration for a new module layer deriving from the boot configuration, and resolving
        //the JPMS module.
//        Configuration cfg = ModuleLayer.boot().configuration().resolve(mf,ModuleFinder.of(), Set.of("io.github.nhtuan10.sample.api.service"));
        Configuration cfg = ModuleLayer.boot().configuration().resolve(mf, ModuleFinder.of(), Set.of("io.github.nhtuan10.sample.plugin1"));
        Module unnamed = Maven.class.getClassLoader().getUnnamedModule();
        ModuleLayer ml = ModuleLayer.boot().defineModulesWithOneLoader(cfg, new DefaultModularClassLoader(List.of("modular-sample-core"),
                dependencyLocations));
        ml.modules().forEach(module -> {
            final Set<String> packages = unnamed.getPackages();
            for (String eachPackage : packages) {
                unnamed.addOpens(eachPackage, module);
                log.info("--add-open " + eachPackage + " from " + unnamed + " to " + module);
            }
        });
//        Class<?> c = ml.findLoader("io.github.nhtuan10.sample.api.service").loadClass(SampleService2.class.getName());
        Class<?> c = ml.findLoader("io.github.nhtuan10.sample.plugin1").loadClass(ServiceImpl.class.getName());
        log.info("Class: " + c);
    }
}

package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Builder;

import java.util.List;
import java.util.Set;

@Builder
public record ModuleLoadConfiguration(List<String> locationUris, String mainClass, List<String> packagesToScan,
                                      ExternalContainer externalContainer, boolean awaitMainClass,
                                      boolean allowNonAnnotatedServices, String modularClassLoaderName,
                                      Set<String> prefixesLoadedBySystemClassLoader,
                                      ModularClassLoader modularClassLoader) {
}
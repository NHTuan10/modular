package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public final class ModuleLoadConfiguration {
    private final List<String> locationUris;
    private final String mainClass;
    private final List<String> packagesToScan;
    private final ExternalContainer externalContainer;
    private final boolean awaitMainClass;
    private final boolean allowNonAnnotatedServices;
    private final String modularClassLoaderName;
    private final Set<String> prefixesLoadedBySystemClassLoader;
    private final ModularClassLoader modularClassLoader;


    public List<String> locationUris() {
        return locationUris;
    }

    public String mainClass() {
        return mainClass;
    }

    public List<String> packagesToScan() {
        return packagesToScan;
    }

    public ExternalContainer externalContainer() {
        return externalContainer;
    }

    public boolean awaitMainClass() {
        return awaitMainClass;
    }

    public boolean allowNonAnnotatedServices() {
        return allowNonAnnotatedServices;
    }

    public String modularClassLoaderName() {
        return modularClassLoaderName;
    }

    public Set<String> prefixesLoadedBySystemClassLoader() {
        return prefixesLoadedBySystemClassLoader;
    }

    public ModularClassLoader modularClassLoader() {
        return modularClassLoader;
    }

}
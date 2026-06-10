package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.classloader.ModularClassLoader;
import lombok.*;

import java.net.URI;
import java.util.List;
import java.util.Set;

@Builder
@RequiredArgsConstructor
@EqualsAndHashCode
@ToString
public final class ModuleLoadConfiguration {
    private final List<URI> locationUris;
    private final String mainClass;
    @Builder.Default
    private final String[] mainMethodArguments = new String[]{};
    private final String entryPointClass;
    @Builder.Default
    private final Object entryPointArgument = null;
    @Builder.Default
    private final String entryPointArgumentInJsonString = null;
    private final boolean awaitModule;
    @Builder.Default
    private final List<String> packagesToScan = List.of();
    private final ExternalContainer externalContainer;
    @Builder.Default
    private final boolean allowNonAnnotatedServices = true;
    @Builder.Default
    @Getter
    private final boolean isOverride = false;
    private final String modularClassLoaderName;
    private final Set<String> prefixesLoadedBySystemClassLoader;
    private final ModularClassLoader modularClassLoader;
    private final boolean doesIncludeSystemClasspath;
    @Builder.Default
    private final ClassLoader parentClassLoader = ClassLoader.getPlatformClassLoader();
    @Builder.Default
    private final String workingDir = System.getProperty("java.io.tmpdir") + "/modular";

    public List<URI> locationUris() {
        return locationUris;
    }

    public String mainClass() {
        return mainClass;
    }

    public String entryPointClass() {
        return entryPointClass;
    }

    public String[] mainMethodArguments() {
        return mainMethodArguments;
    }

    public Object entryPointArgument() {
        return entryPointArgument;
    }

    public List<String> packagesToScan() {
        return packagesToScan;
    }

    public ExternalContainer externalContainer() {
        return externalContainer;
    }

    public boolean awaitModule() {
        return awaitModule;
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

    public boolean doesIncludeSystemClasspath() {
        return doesIncludeSystemClasspath;
    }

    public ClassLoader parentClassLoader() {
        return parentClassLoader;
    }

    public String workingDir() {
        return workingDir;
    }

    public String entryPointArgumentInJsonString() {
        return entryPointArgumentInJsonString;
    }
}
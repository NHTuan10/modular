package io.github.nhtuan10.modular.api.module;

import lombok.Builder;

import java.util.List;

@Builder
public record ModuleLoadConfiguration(List<String> locationUris, String mainClass, List<String> packagesToScan,
                                      ExternalContainer externalContainer, boolean awaitMainClass,
                                      boolean allowNonAnnotatedServices) {
}
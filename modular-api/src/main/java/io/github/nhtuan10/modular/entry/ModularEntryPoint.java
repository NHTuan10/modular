package io.github.nhtuan10.modular.entry;

import io.github.nhtuan10.modular.api.annotation.ModularService;

@ModularService
public interface ModularEntryPoint<P, R> {
    R run(P parameter) throws Exception;

    default void onUnloadModule() {
        // No operation
    }
}



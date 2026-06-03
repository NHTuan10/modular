package io.github.nhtuan10.modular.entry;

import io.github.nhtuan10.modular.api.annotation.ModularService;

@ModularService
public interface ModularEntryPoint<T, R> {
    R run(T parameter) throws Exception;
}



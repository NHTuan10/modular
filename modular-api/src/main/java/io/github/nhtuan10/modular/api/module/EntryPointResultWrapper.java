package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.entry.ModularEntryPoint;
import lombok.Data;

@Data
public class EntryPointResultWrapper {
    private final ModularEntryPoint modularEntryPoint;
    private final Object entryPointResult;
}
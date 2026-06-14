package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.entry.ModularEntryPoint;

public record EntryPointResultWrapper(ModularEntryPoint<?, ?> modularEntryPoint, Object entryPointResult) {
}
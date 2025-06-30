package io.github.nhtuan10.modular.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@EqualsAndHashCode(exclude = {"interfaceClass"})
@Getter
@AllArgsConstructor
public final class ModularServiceHolder {
    private final Class<?> serviceClass;
    private final String name;
    private final Object instance;
    private final Class<?> interfaceClass;

}

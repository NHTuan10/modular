package io.github.nhtuan10.modular.model;

import io.github.nhtuan10.modular.api.module.ExternalContainer;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

@ToString
//@EqualsAndHashCode(exclude = {"interfaceClass"})
@EqualsAndHashCode
@Getter
public final class ModularServiceHolder {
    private final String moduleName;
    @Setter
    private Class<?> serviceClass;
    private final String name;
    @Setter
    private Object instance;
    private final Set<Class<?>> interfaceClasses;
    @Setter
    private ExternalContainer externalContainer;
    @Setter
    private String externalBeanName;
    private final ClassLoader classLoader;

    public ModularServiceHolder(String moduleName, Class<?> serviceClass, String name, Set<Class<?>> interfaceClasses, ExternalContainer externalContainer, ClassLoader classLoader) {
        this.moduleName = moduleName;
        this.serviceClass = serviceClass;
        this.name = name;
        this.interfaceClasses = interfaceClasses;
        this.externalContainer = externalContainer;
        this.classLoader = classLoader;
    }

    public ModularServiceHolder(String moduleName, Class<?> serviceClass, String name, Object instance, Set<Class<?>> interfaceClasses, ClassLoader classLoader) {
        this.moduleName = moduleName;
        this.name = name;
        this.serviceClass = serviceClass;
        this.instance = instance;
        this.interfaceClasses = interfaceClasses;
        this.classLoader = classLoader;
    }
}

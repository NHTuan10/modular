package io.github.nhtuan10.modular.api.model;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

public interface ModularContext {
    void notifyModuleReady();
    <S> List<S> getModularServicesFromSpring(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;
    <S> List<S> getModularServices(Class<?> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException;
}

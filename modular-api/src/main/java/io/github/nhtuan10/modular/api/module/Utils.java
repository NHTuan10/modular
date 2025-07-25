package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static <T> Class<T> getImplementationClass(Class<T> interfaceClass, Class<?> caller) {
        try (InputStream is = ModuleLoader.class.getClassLoader().getResourceAsStream("META-INF/services/" + interfaceClass.getName())) {
            if (is != null) {
                String className = new String(is.readAllBytes());
                @SuppressWarnings("unchecked")
                Class<T> tClass = (Class<T>) Class.forName(className, true, caller.getClassLoader());
                return tClass;
            } else {
                throw new ModularRuntimeException("Couldn't find any implementation class for " + interfaceClass);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ModularRuntimeException("Couldn't find any implementation class for " + interfaceClass, e);
        }
    }
}

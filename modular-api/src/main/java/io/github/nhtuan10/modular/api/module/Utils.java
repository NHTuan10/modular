package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModuleLoadRuntimeException;

import java.io.IOException;
import java.io.InputStream;

public class Utils {
    public static <T> Class<T> getImplementationClass(Class<T> interfaceClass, Class<?> caller) {
//        ServiceLoader<ModuleLoader> loader = ServiceLoader.load(ModuleLoader.class);
//        if (loader.findFirst().isPresent()) {
//            ModuleLoader moduleLoader = loader.findFirst().get();
//            return moduleLoader.getClass();
//        }
//        else {
//            throw new ModuleLoadRuntimeException("Couldn't find any ModuleLoader implementation class");
//        }
        try (InputStream is = ModuleLoader.class.getClassLoader().getResourceAsStream("META-INF/services/" + interfaceClass.getName())) {
            if (is != null) {
                String clazz = new String(is.readAllBytes());
                return (Class<T>) Class.forName(clazz, true, caller.getClassLoader());
            } else {
                throw new ModuleLoadRuntimeException("Couldn't find any implementation class for " + interfaceClass);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ModuleLoadRuntimeException("Couldn't find any implementation class for " + interfaceClass, e);
        }
    }
}

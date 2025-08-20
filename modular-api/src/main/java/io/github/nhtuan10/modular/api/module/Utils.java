package io.github.nhtuan10.modular.api.module;

import io.github.nhtuan10.modular.api.exception.ModularRuntimeException;
import io.github.nhtuan10.modular.api.exception.SerializationRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceInvocationRuntimeException;
import io.github.nhtuan10.modular.api.serdeserializer.JavaSerDeserializer;
import io.github.nhtuan10.modular.api.serdeserializer.SerDeserializer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;

public class Utils {
    public static <T> Class<T> getImplementationClass(Class<T> interfaceClass, ClassLoader classLoader) {
        try (InputStream is = ModuleLoader.class.getClassLoader().getResourceAsStream("META-INF/services/" + interfaceClass.getName())) {
            if (is != null) {
                String className = new String(is.readAllBytes());
                @SuppressWarnings("unchecked")
                Class<T> tClass = (Class<T>) Class.forName(className, true, classLoader);
//                Class<T> tClass = (Class<T>) classLoader.loadClass(className);
//                Class<T> tClass = (Class<T>) ClassLoader.getSystemClassLoader().loadClass(className);
                return tClass;
            } else {
                throw new ModularRuntimeException("Couldn't find any implementation class for " + interfaceClass);
            }
        } catch (IOException | ClassNotFoundException e) {
            throw new ModularRuntimeException("Couldn't find any implementation class for " + interfaceClass, e);
        }
    }

    public static <I> I createProxyObject(Class<I> apiClass, Object target) throws InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchFieldException, NoSuchMethodException {
        SerDeserializer serDeserializer = new JavaSerDeserializer();
        ClassLoader sourceClassLoader = apiClass.getClassLoader();
        Object svcInvocationInterceptor = new ServiceInvocationInterceptor(target, sourceClassLoader, serDeserializer);
        Object equalsMethodInterceptor = new EqualsMethodInterceptor(target);
        Class<? extends I> c = new ByteBuddy()
                .subclass(apiClass)
                //                .name(apiClass.get() + "$Proxy") // will uncomment it out when does the Graalvm POC
                .method(ElementMatchers.isEquals())
                .intercept(MethodDelegation.to(equalsMethodInterceptor))
                .method(ElementMatchers.any().and(ElementMatchers.not(ElementMatchers.isEquals())))
                .intercept(MethodDelegation.to(svcInvocationInterceptor))
                .defineField(ModuleLoader.PROXY_TARGET_FIELD_NAME, Object.class, Visibility.PRIVATE)
                .make()
                .load(sourceClassLoader)

                .getLoaded();
        I proxy = c.getConstructor().newInstance();

        Field targetField = c.getDeclaredField(ModuleLoader.PROXY_TARGET_FIELD_NAME);
        targetField.setAccessible(true);
        targetField.set(proxy, target);
        return proxy;

    }

    @RequiredArgsConstructor
    public static class ServiceInvocationInterceptor {
        private final Object target;
        private final ClassLoader sourceClassLoader;
        private final SerDeserializer serDeserializer;

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method) throws Exception {
            // intercept any method of any signature
            ClassLoader targetClassLoader = target.getClass().getClassLoader();
            String serviceClassName = target.getClass().getName();
            Class<?>[] serviceClassLoaderParameterTypes = (Class<?>[]) Arrays.stream(method.getParameterTypes())
//                .map(Class::getName)
                    .map(clazz -> {
                        try {
                            if (!clazz.isPrimitive()) {
                                return targetClassLoader.loadClass(clazz.getName());
                            } else return clazz;
                        } catch (Exception e) {
                            throw new ServiceInvocationRuntimeException(String.format("Failed to serialize parameter type '%s' from class '%s', method '%s'", clazz, serviceClassName, method), e);
                        }
                    })
                    .toArray(Class[]::new);
            Method serviceClassLoaderMethod;
            try {
                serviceClassLoaderMethod = target.getClass().getMethod(method.getName(), serviceClassLoaderParameterTypes);
                serviceClassLoaderMethod.setAccessible(true);
            } catch (NoSuchMethodException e) {
                throw new ServiceInvocationRuntimeException(String.format("No method found for method '%s' in service class '%s'", method, serviceClassName), e);
            }
            Object[] convertedArgs = IntStream.range(0, serviceClassLoaderParameterTypes.length).mapToObj(i -> {
                try {
                    if (allArguments[i] instanceof Class) {
                        return allArguments[i];
                    } else
                        return serDeserializer.castWithSerialization(allArguments[i], targetClassLoader);
                } catch (Exception e) {
                    throw new SerializationRuntimeException(String.format("Failed to serialize argument type '%s' from class '%s', method '%s'", serviceClassLoaderParameterTypes[i], serviceClassName, method), e);
                }
            }).toArray();
            try {
                Object result = serviceClassLoaderMethod.invoke(target, convertedArgs);
                return serDeserializer.castWithSerialization(result, sourceClassLoader);
            } catch (InvocationTargetException e) {
                throw e;
            } catch (Exception e) {
                throw new ServiceInvocationRuntimeException(String.format("Failed to invoke method '%s' in service class '%s'", method, serviceClassName), e);
            }
        }
    }

    @AllArgsConstructor
    public static class EqualsMethodInterceptor {
        private Object service;

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @This Object object) {
            Object comparingObj = allArguments[0];
            // compare with ByteBuddy generated proxy object and target service object to see any match
            return object == comparingObj || service.equals(comparingObj);
        }
    }
}

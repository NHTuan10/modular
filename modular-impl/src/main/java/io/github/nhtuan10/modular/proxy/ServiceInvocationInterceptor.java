package io.github.nhtuan10.modular.proxy;

import io.github.nhtuan10.modular.api.exception.SerializationRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceInvocationRuntimeException;
import io.github.nhtuan10.modular.serdeserializer.SerDeserializer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ServiceInvocationInterceptor {
    private final Object service;
    private final SerDeserializer serDeserializer;

    @RuntimeType
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method, @This Object object) {
        // intercept any method of any signature
        String serviceClassName = service.getClass().getName();
        Class<?>[] parameterTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getName)
                .map(s -> {
                    try {
                        return service.getClass().getClassLoader().loadClass(s);
                    } catch (ClassNotFoundException e) {
                        throw new ServiceInvocationRuntimeException("Class not found for parameter type '%s' from class '%s', method '%s'".formatted(s, serviceClassName, method), e);
                    }
                })
                .toArray(Class[]::new);
        Method m = null;
        try {
            m = service.getClass().getMethod(method.getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ServiceInvocationRuntimeException("No method found for method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
        Object[] convertedArgs = IntStream.range(0, parameterTypes.length).mapToObj(i -> {
            try {
                return serDeserializer.castWithSerialization(allArguments[i], parameterTypes[i].getClassLoader());
            } catch (Exception e) {
                throw new SerializationRuntimeException("Failed to serialize argument type '%s' from class '%s', method '%s'".formatted(parameterTypes[i], serviceClassName, method), e);
            }

        }).toArray();
        try {
            Object result = m.invoke(service, convertedArgs);
            return serDeserializer.castWithSerialization(result, this.getClass().getClassLoader());
        } catch (Exception e) {
            throw new ServiceInvocationRuntimeException("Failed to invoke method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
    }

    @AllArgsConstructor
    public static class EqualsMethodInterceptor {
        private Object service;

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method, @This Object object) {
            Object comparingObj = allArguments[0];
            // compare with ByteBuddy generated proxy object and target service object to see any match
            return object == comparingObj || service.equals(comparingObj);
        }
    }
}
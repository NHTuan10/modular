package io.github.nhtuan10.modular.proxy;

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;
import io.github.nhtuan10.modular.api.exception.ModularSerializationException;
import io.github.nhtuan10.modular.api.exception.ModularServiceInvocationException;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.implementation.bind.annotation.*;
import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ServiceInvocationInterceptor {
    //  private ModularServiceHolder modularServiceHolder;
    private final Object service;
    private final SerDeserializer serDeserializer;
//  public ServiceInvocationInterceptor(ModularServiceHolder modularServiceHolder) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
//    this.modularServiceHolder = modularServiceHolder;
//    service = modularServiceHolder.getServiceClass().getConstructor().newInstance();
//
//  }

    @RuntimeType
//    @SuperCall Callable<?> superCall, @Super Object superObject
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
                        throw new ModularServiceInvocationException("Class not found for parameter type '%s' from class '%s', method '%s'".formatted(s, serviceClassName, method), e);
                    }
                })
                .toArray(Class[]::new);
        Method m = null;
        try {
            m = service.getClass().getMethod(method.getName(), parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ModularServiceInvocationException("No method found for method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
        Object[] convertedArgs = IntStream.range(0, parameterTypes.length).mapToObj(i -> {
            try {
                return serDeserializer.castWithSerialization(allArguments[i], parameterTypes[i]);
            } catch (Exception e) {
                throw new ModularSerializationException("Failed to serialize argument type '%s' from class '%s', method '%s'".formatted(parameterTypes[i], serviceClassName, method), e);
            }

        }).toArray();
        try {
            Object result = m.invoke(service, convertedArgs);
            return serDeserializer.castWithSerialization(result, method.getReturnType());
        } catch (Exception e) {
            throw new ModularServiceInvocationException("Failed to invoke method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
    }

    @AllArgsConstructor
    public static class EqualsMethodInterceptor {
        private Object service;

        @RuntimeType
        public Object intercept(@AllArguments Object[] allArguments,
                                @Origin Method method, @This Object object) {
            // intercept any method of any signature
            Object comparingObj = allArguments[0];
            // compare with ByteBuddy generated proxy object or target service object
            return object == comparingObj || service.equals(comparingObj);
        }
    }
}
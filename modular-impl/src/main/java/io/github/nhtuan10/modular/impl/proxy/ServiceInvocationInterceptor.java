package io.github.nhtuan10.modular.impl.proxy;

import io.github.nhtuan10.modular.api.exception.SerializationRuntimeException;
import io.github.nhtuan10.modular.api.exception.ServiceInvocationRuntimeException;
import io.github.nhtuan10.modular.impl.experimental.ProxyCreator;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.IntStream;

@Slf4j
@RequiredArgsConstructor
public class ServiceInvocationInterceptor {
    private final Object service;
    private final SerDeserializer serDeserializer;
    private final boolean copyTransClassLoaderObjects;
    private final ClassLoader sourceClassLoader;
    private final ClassLoader targetClassLoader;
    @RuntimeType
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method) throws Exception {
        // TODO: need to exclude packages having issues when open
        // intercept any method of any signature
        String serviceClassName = service.getClass().getName();
        Class<?>[] serviceClassLoaderParameterTypes = (Class<?>[]) Arrays.stream(method.getParameterTypes())
//                .map(Class::getName)
                .map(clazz -> {
                    try {
                        addOpenToJpmsSerDeserializerModule(clazz);
                        Class<?> targetClass = sourceClassLoader == targetClassLoader ? clazz : (Class<?>) serDeserializer.castWithSerialization(clazz, targetClassLoader);
                        addOpenToJpmsSerDeserializerModule(targetClass);
                        return targetClass;
//                        return service.getClass().getClassLoader().loadClass(clazz);
                    }
//                    catch (ClassNotFoundException e) {
//                        throw new ServiceInvocationRuntimeException("Class not found for parameter type '%s' from class '%s', method '%s'".formatted(clazz, serviceClassName, method), e);
//                    }
                    catch (Exception e) {
                        throw new ServiceInvocationRuntimeException(String.format("Failed to serialize parameter type '%s' from class '%s', method '%s'", clazz, serviceClassName, method), e);
                    }
                })
                .toArray(Class[]::new);
        Method serviceClassLoaderMethod;
        try {
            serviceClassLoaderMethod = service.getClass().getMethod(method.getName(), serviceClassLoaderParameterTypes);
            serviceClassLoaderMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ServiceInvocationRuntimeException(String.format("No method found for method '%s' in service class '%s'", method, serviceClassName), e);
        }
        Object[] convertedArgs = IntStream.range(0, serviceClassLoaderParameterTypes.length).mapToObj(i -> {
            try {
//                return copyTransClassLoaderObjects ? serDeserializer.castWithSerialization(allArguments[i], serviceClassLoaderParameterTypes[i].getClassLoader())
//                        : ProxyCreator.createProxyObject(serviceClassLoaderParameterTypes[i], allArguments[i], serDeserializer, false, service.getClass().getClassLoader());
                return cast(allArguments[i], serviceClassLoaderParameterTypes[i], sourceClassLoader, targetClassLoader);
            } catch (Exception e) {
                throw new SerializationRuntimeException(String.format("Failed to serialize argument type '%s' from class '%s', method '%s'", serviceClassLoaderParameterTypes[i], serviceClassName, method), e);
            }

        }).toArray();
        try {
            Object result = serviceClassLoaderMethod.invoke(service, convertedArgs);
            if (result != null) {
                addOpenToJpmsSerDeserializerModule(result.getClass());
            }
//            return copyTransClassLoaderObjects ? serDeserializer.castWithSerialization(result, this.getClass().getClassLoader())
//                    : (result != null ? ProxyCreator.createProxyObject(method.getReturnType(), result, serDeserializer, false, this.getClass().getClassLoader()) : null);
            return cast(result, method.getReturnType(), targetClassLoader, sourceClassLoader);
        } catch (InvocationTargetException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceInvocationRuntimeException(String.format("Failed to invoke method '%s' in service class '%s'", method, serviceClassName), e);
        }
    }

    private void addOpenToJpmsSerDeserializerModule(Class<?> clazz) {
        Module module = clazz.getModule();
        Module serdesModule = serDeserializer.getJpmsModule();
        final Set<String> modulePackages = module.getPackages();
        for (String eachPackage : modulePackages) {
            try {
                module.addOpens(eachPackage, serdesModule);
                log.debug("--add-open " + eachPackage + " from " + module + " to " + serdesModule);
            } catch (Exception e) {
                log.debug("Cannot add opens package {} from module {} to module {}", eachPackage, module, serdesModule, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private Object cast(Object obj, Class<?> type, ClassLoader sourceClassLoader, ClassLoader targetClassLoader) throws Exception {
        if (ServiceProxyCreator.isConversionNeeded(obj, type, sourceClassLoader, targetClassLoader)) {
            return obj;
        }
        if (copyTransClassLoaderObjects) {
            return serDeserializer.castWithSerialization(obj, targetClassLoader);
        } else {
            return ProxyCreator.createProxyObject(obj, type, sourceClassLoader, targetClassLoader, serDeserializer);
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
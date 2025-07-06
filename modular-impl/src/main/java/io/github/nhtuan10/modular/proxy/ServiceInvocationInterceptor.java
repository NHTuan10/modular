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

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.IntStream;

@RequiredArgsConstructor
public class ServiceInvocationInterceptor {
    private final Object service;
    private final SerDeserializer serDeserializer;
    private final boolean copyTransClassLoaderObjects;
    private final ClassLoader serviceClassLoader;

    @RuntimeType
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method) {
        // intercept any method of any signature
        String serviceClassName = service.getClass().getName();
        Class<?>[] serviceClassLoaderParameterTypes = (Class<?>[]) Arrays.stream(method.getParameterTypes())
//                .map(Class::getName)
                .map(clazz -> {
                    try {
                        return serDeserializer.castWithSerialization(clazz, serviceClassLoader);
//                        return service.getClass().getClassLoader().loadClass(clazz);
                    }
//                    catch (ClassNotFoundException e) {
//                        throw new ServiceInvocationRuntimeException("Class not found for parameter type '%s' from class '%s', method '%s'".formatted(clazz, serviceClassName, method), e);
//                    }
                    catch (Exception e) {
                        throw new ServiceInvocationRuntimeException("Failed to serialize parameter type '%s' from class '%s', method '%s'".formatted(clazz, serviceClassName, method), e);
                    }
                })
                .toArray(Class[]::new);
        Method serviceClassLoaderMethod;
        try {
            serviceClassLoaderMethod = service.getClass().getMethod(method.getName(), serviceClassLoaderParameterTypes);
            serviceClassLoaderMethod.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new ServiceInvocationRuntimeException("No method found for method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
        Object[] convertedArgs = IntStream.range(0, serviceClassLoaderParameterTypes.length).mapToObj(i -> {
            try {
//                return copyTransClassLoaderObjects ? serDeserializer.castWithSerialization(allArguments[i], serviceClassLoaderParameterTypes[i].getClassLoader())
//                        : ProxyCreator.createProxyObject(serviceClassLoaderParameterTypes[i], allArguments[i], serDeserializer, false, service.getClass().getClassLoader());
                return cast(allArguments[i], serviceClassLoaderParameterTypes[i], this.getClass().getClassLoader(), serviceClassLoader);
            } catch (Exception e) {
                throw new SerializationRuntimeException("Failed to serialize argument type '%s' from class '%s', method '%s'".formatted(serviceClassLoaderParameterTypes[i], serviceClassName, method), e);
            }

        }).toArray();
        try {
            Object result = serviceClassLoaderMethod.invoke(service, convertedArgs);
//            return copyTransClassLoaderObjects ? serDeserializer.castWithSerialization(result, this.getClass().getClassLoader())
//                    : (result != null ? ProxyCreator.createProxyObject(method.getReturnType(), result, serDeserializer, false, this.getClass().getClassLoader()) : null);
            return cast(result, method.getReturnType(), serviceClassLoader, this.getClass().getClassLoader());
        } catch (Exception e) {
            throw new ServiceInvocationRuntimeException("Failed to invoke method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object cast(Object obj, Class<?> type, ClassLoader sourceClassLoader, ClassLoader targetClassLoader) throws Exception {
        if (copyTransClassLoaderObjects) {
            return serDeserializer.castWithSerialization(obj, targetClassLoader);
        } else {
            if (obj == null) return null;
            if (type.isPrimitive() || type.equals(String.class)) {
                return obj;
            } else if (type.isEnum() || type.isRecord()) {
                return serDeserializer.castWithSerialization(obj, targetClassLoader);
            } else if (type.isArray()) {
                Object[] array = (Object[]) obj;
                Object[] castedArray = (Object[]) Array.newInstance(type.getComponentType(), array.length);
                for (int i = 0; i < array.length; i++) {
                    castedArray[i] = cast(array[i], type.getComponentType(), sourceClassLoader, targetClassLoader);
                }
                return castedArray;
            }
            // TODO: try open java.util classes for reflections
            else if (Collection.class.isAssignableFrom(type)) {

                if (Set.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    Set castedCollection = HashSet.class.getConstructor().newInstance();
                    if (!collection.isEmpty()) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(new ArrayList<>(collection).get(0).getClass(), targetClassLoader);
                        for (Object item : collection) {
                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                        }
                    }
                    return Collections.unmodifiableSet(castedCollection);
                } else if (Queue.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    Queue castedCollection = LinkedList.class.getConstructor().newInstance();
                    if (!collection.isEmpty()) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(new ArrayList<>(collection).get(0).getClass(), targetClassLoader);
                        for (Object item : collection) {
                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                        }
                    }
                    return castedCollection;
                } else if (Collection.class.equals(type) || List.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    List castedCollection = ArrayList.class.getConstructor().newInstance();
                    if (!collection.isEmpty()) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(new ArrayList<>(collection).get(0).getClass(), targetClassLoader);
                        for (Object item : collection) {
                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                        }
                    }
                    return Collections.unmodifiableList(castedCollection);
                } else {
                    Collection collection = (Collection) obj;
                    Class<? extends Collection> c = (Class<? extends Collection>) obj.getClass();
                    Collection castedCollection = c.getConstructor().newInstance();
                    if (!collection.isEmpty()) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(new ArrayList<>(collection).get(0).getClass(), targetClassLoader);
                        for (Object item : collection) {
                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                        }
                    }
                    return castedCollection;
                }
            } else if (Map.class.isAssignableFrom(type)) {
                Map map = (Map) obj;
                Map castedMap = HashMap.class.getConstructor().newInstance();
                for (Object key : map.keySet()) {
                    castedMap.put(cast(key, (Class<?>) serDeserializer.castWithSerialization(obj.getClass().getComponentType(), targetClassLoader), sourceClassLoader, targetClassLoader), cast(map.get(key), type.getComponentType(), sourceClassLoader, targetClassLoader));
                }
                return Collections.unmodifiableMap(castedMap);
            } else if (type.getClassLoader() == null) {
                // TODO: revise this implementation to support type loaded by bootstrap class loader
                return serDeserializer.castWithSerialization(obj, targetClassLoader);
            }
            else {
                return ProxyCreator.createProxyObject(type, obj, serDeserializer, false, targetClassLoader, sourceClassLoader);
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
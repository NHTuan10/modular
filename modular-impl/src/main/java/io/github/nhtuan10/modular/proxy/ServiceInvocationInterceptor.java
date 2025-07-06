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
    private final ClassLoader sourceClassLoader;
    private final ClassLoader targetClassLoader;

    @RuntimeType
    public Object intercept(@AllArguments Object[] allArguments,
                            @Origin Method method) {
        // intercept any method of any signature
        String serviceClassName = service.getClass().getName();
        Class<?>[] serviceClassLoaderParameterTypes = (Class<?>[]) Arrays.stream(method.getParameterTypes())
//                .map(Class::getName)
                .map(clazz -> {
                    try {
                        return serDeserializer.castWithSerialization(clazz, targetClassLoader);
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
                return cast(allArguments[i], serviceClassLoaderParameterTypes[i], sourceClassLoader, targetClassLoader);
            } catch (Exception e) {
                throw new SerializationRuntimeException("Failed to serialize argument type '%s' from class '%s', method '%s'".formatted(serviceClassLoaderParameterTypes[i], serviceClassName, method), e);
            }

        }).toArray();
        try {
            Object result = serviceClassLoaderMethod.invoke(service, convertedArgs);
//            return copyTransClassLoaderObjects ? serDeserializer.castWithSerialization(result, this.getClass().getClassLoader())
//                    : (result != null ? ProxyCreator.createProxyObject(method.getReturnType(), result, serDeserializer, false, this.getClass().getClassLoader()) : null);
            return cast(result, method.getReturnType(), targetClassLoader, sourceClassLoader);
        } catch (Exception e) {
            throw new ServiceInvocationRuntimeException("Failed to invoke method '%s' in service class '%s'".formatted(method, serviceClassName), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object cast(Object obj, Class<?> type, ClassLoader sourceClassLoader, ClassLoader targetClassLoader) throws Exception {
        if (copyTransClassLoaderObjects) {
            return serDeserializer.castWithSerialization(obj, targetClassLoader);
        } else {
            if (obj == null) {
                return null;
            } else if (targetClassLoader.equals(sourceClassLoader)) {
                return obj;
            }
            if (type.isPrimitive() || isBoxedPrimitive(type) || type.equals(String.class)) {
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
//            else if (Collection.class.isAssignableFrom(type)) {
//                Collection collection = (Collection) obj;
//                if (!collection.isEmpty()) {
//                    ClassLoader itemCL = collection.iterator().next().getClass().getClassLoader();
//                    return ProxyCreator.createProxyObject(type, obj, serDeserializer, false, itemCL, sourceClassLoader);
//                } else {
//                    return obj;
//                }
//            }
            else if (Collection.class.isAssignableFrom(type)) {

                if (Set.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    Set castedCollection = HashSet.class.getConstructor().newInstance();
                    if (!collection.isEmpty()) {
                        for (Object item : collection) {
                            Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                        }
                    }
                    return Collections.unmodifiableSet(castedCollection);
                } else if (Queue.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    Queue castedCollection = LinkedList.class.getConstructor().newInstance();
                    for (Object item : collection) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                    }
                    return castedCollection;
                } else if (Collection.class.equals(type) || List.class.isAssignableFrom(type)) {
                    Collection collection = (Collection) obj;
                    List castedCollection = ArrayList.class.getConstructor().newInstance();
                    for (Object item : collection) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                    }
                    return Collections.unmodifiableList(castedCollection);
                } else {
                    Collection collection = (Collection) obj;
                    Class<? extends Collection> c = (Class<? extends Collection>) obj.getClass();
                    Collection castedCollection = c.getConstructor().newInstance();
                    for (Object item : collection) {
                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
                    }
                    return castedCollection;
                }
            } else if (Map.class.isAssignableFrom(type)) {
                Map map = (Map) obj;
                Map castedMap = HashMap.class.getConstructor().newInstance();
                for (Object key : map.keySet()) {
                    Class<?> keyType = (Class<?>) serDeserializer.castWithSerialization(key.getClass(), targetClassLoader);
                    if (map.get(key) == null) {
                        castedMap.put(cast(key, keyType, sourceClassLoader, targetClassLoader), null);
                    } else {
                        Class<?> valueType = (Class<?>) serDeserializer.castWithSerialization(map.get(key).getClass(), targetClassLoader);
                        castedMap.put(cast(key, keyType, sourceClassLoader, targetClassLoader), cast(map.get(key), valueType, sourceClassLoader, targetClassLoader));
                    }
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

    private boolean isBoxedPrimitive(Class<?> type) {
        return type == Integer.class ||
                type == Long.class ||
                type == Short.class ||
                type == Byte.class ||
                type == Float.class ||
                type == Double.class ||
                type == Boolean.class ||
                type == Character.class;
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
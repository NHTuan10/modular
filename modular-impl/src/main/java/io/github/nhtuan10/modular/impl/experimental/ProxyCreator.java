package io.github.nhtuan10.modular.impl.experimental;

import io.github.nhtuan10.modular.impl.proxy.ServiceProxyCreator;
import io.github.nhtuan10.modular.impl.serdeserializer.SerDeserializer;

import java.lang.reflect.Array;
import java.util.*;

public class ProxyCreator {

    public static Object createProxyObject(Object obj, Class<?> type, ClassLoader sourceClassLoader, ClassLoader targetClassLoader, SerDeserializer serDeserializer) throws Exception {
        if (ServiceProxyCreator.isConversionNeeded(obj, type, sourceClassLoader, targetClassLoader)) {
            return obj;
        }
        if (type.isEnum() || type.isRecord()) {
            return serDeserializer.castWithSerialization(obj, targetClassLoader);
        } else if (type.isArray()) {
            Object[] array = (Object[]) obj;
            Object[] castedArray = (Object[]) Array.newInstance(type.getComponentType(), array.length);
            for (int i = 0; i < array.length; i++) {
                castedArray[i] = createProxyObject(array[i], type.getComponentType(), sourceClassLoader, targetClassLoader, serDeserializer);
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
            Map<Class<? extends Collection>, Class<? extends Collection>> map = Map.of(Set.class, HashSet.class, Queue.class, LinkedList.class, List.class, ArrayList.class, Collection.class, ArrayList.class);
            Collection<?> collection = (Collection<?>) obj;
            if (!collection.isEmpty()) {
                Class<? extends Collection> clazz = map.entrySet().stream()
                        .filter(c -> c.getKey().isAssignableFrom(type))
                        .map(Map.Entry::getValue).findFirst()
                        .orElse(null);
                if (clazz == null) {
                    clazz = (Class<? extends Collection>) obj.getClass();
                }
                Collection<Object> castedCollection = clazz.getConstructor().newInstance();
                for (Object item : collection) {
                    Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
                    castedCollection.add(createProxyObject(item, itemType, sourceClassLoader, targetClassLoader, serDeserializer));
                }
                return castedCollection;
            } else {
                return obj;
            }

            //                if (Set.class.isAssignableFrom(type)) {
//                    Collection collection = (Collection) obj;
//                    Set castedCollection = HashSet.class.getConstructor().newInstance();
//                    if (!collection.isEmpty()) {
//                        for (Object item : collection) {
//                            Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
//                            castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
//                        }
//                    }
//                    return Collections.unmodifiableSet(castedCollection);
//                } else if (Queue.class.isAssignableFrom(type)) {
//                    Collection collection = (Collection) obj;
//                    Queue castedCollection = LinkedList.class.getConstructor().newInstance();
//                    for (Object item : collection) {
//                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
//                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
//                    }
//                    return castedCollection;
//                } else if (Collection.class.equals(type) || List.class.isAssignableFrom(type)) {
//                    Collection collection = (Collection) obj;
//                    List castedCollection = ArrayList.class.getConstructor().newInstance();
//                    for (Object item : collection) {
//                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
//                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
//                    }
//                    return Collections.unmodifiableList(castedCollection);
//                } else {
//                    Collection collection = (Collection) obj;
//                    Class<? extends Collection> c = (Class<? extends Collection>) obj.getClass();
//                    Collection castedCollection = c.getConstructor().newInstance();
//                    for (Object item : collection) {
//                        Class<?> itemType = (Class<?>) serDeserializer.castWithSerialization(item.getClass(), targetClassLoader);
//                        castedCollection.add(cast(item, itemType, sourceClassLoader, targetClassLoader));
//                    }
//                    return Collections.unmodifiableCollection(castedCollection);
//                }
        } else if (Map.class.isAssignableFrom(type)) {
            Map map = (Map) obj;
            Map castedMap = HashMap.class.getConstructor().newInstance();
            for (Object key : map.keySet()) {
                Class<?> keyType = (Class<?>) serDeserializer.castWithSerialization(key.getClass(), targetClassLoader);
                if (map.get(key) == null) {
                    castedMap.put(createProxyObject(key, keyType, sourceClassLoader, targetClassLoader, serDeserializer), null);
                } else {
                    Class<?> valueType = (Class<?>) serDeserializer.castWithSerialization(map.get(key).getClass(), targetClassLoader);
                    castedMap.put(createProxyObject(key, keyType, sourceClassLoader, targetClassLoader, serDeserializer), createProxyObject(map.get(key), valueType, sourceClassLoader, targetClassLoader, serDeserializer));
                }
            }
            return Collections.unmodifiableMap(castedMap);
        } else if (type.getClassLoader() == null) {
            // TODO: revise this implementation to support type loaded by bootstrap class loader
            return serDeserializer.castWithSerialization(obj, targetClassLoader);
        } else {
            return ServiceProxyCreator.createProxyObject(type, obj, serDeserializer, false, targetClassLoader, sourceClassLoader);
        }
    }
}

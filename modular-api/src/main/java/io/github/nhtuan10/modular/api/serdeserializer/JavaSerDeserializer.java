package io.github.nhtuan10.modular.api.serdeserializer;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;
import java.util.Collection;

public class JavaSerDeserializer implements SerDeserializer {
    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        if (obj == null)
            return null;
        Class<?> objClass = obj.getClass();
        if (objClass.getClassLoader() == classLoader) {
            return obj;
        }
        //TODO: need a proper implementation for arrays and collections
        if (objClass.isArray() || Collection.class.isAssignableFrom(objClass)) {
            return obj;
        }
//        if (objClass.isArray()){
//            Object[] array = (Object[]) obj;
//            if (array.length == 0 || array[0].getClass().getClassLoader() == classLoader) {
//                return array;
//            }
//        }
//        else if (Collection.class.isAssignableFrom(objClass)){
//            Collection<?> collection = (Collection<?>) obj;
//            if (collection.isEmpty() || collection.iterator().next().getClass().getClassLoader() == classLoader) {
//                return collection;
//            }
//        }
        byte[] b = SerializationUtils.serialize((Serializable) obj);
        InputStream is = new ByteArrayInputStream(b);
        try (ObjectInputStream objectInputStream = new ObjectInputStreamWithClassLoader(is, classLoader)) {
            return objectInputStream.readObject();
        }
    }

    @Override
    public <T> T deserialization(byte[] bytes, Class<T> type) throws Exception {
        if (bytes == null)
            return null;
        InputStream is = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStreamWithClassLoader(is, type.getClassLoader())) {
            return (T) objectInputStream.readObject();
        }
    }

    @Override
    public byte[] serialization(Object obj) {
        return SerializationUtils.serialize((Serializable) obj);
    }

    @Override
    public Module getJpmsModule() {
        return SerializationUtils.class.getModule();
    }

    public static class ObjectInputStreamWithClassLoader extends ObjectInputStream {

        ClassLoader classLoader;

        public ObjectInputStreamWithClassLoader(InputStream in, ClassLoader classLoader) throws IOException {
            super(in);
            this.classLoader = classLoader;
        }

        @Override
        protected Class<?> resolveClass(ObjectStreamClass desc) throws IOException, ClassNotFoundException {
            try {
                return Class.forName(desc.getName(), true, classLoader);
            } catch (Exception e) {
                return super.resolveClass(desc);
            }
        }
    }
}

package io.github.nhtuan10.modular.serdeserializer;

import org.apache.commons.lang3.SerializationUtils;

import java.io.*;

public class JavaSerDeserializer implements SerDeserializer{
    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        byte[] b = SerializationUtils.serialize((Serializable) obj);
        InputStream is = new ByteArrayInputStream(b);
        try (ObjectInputStream objectInputStream = new ObjectInputStreamWithClassLoader(is, classLoader)) {
            return objectInputStream.readObject();
        }
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

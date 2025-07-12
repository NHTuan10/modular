package io.github.nhtuan10.modular.impl.serdeserializer;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;

public class KryoSerDeserializer implements SerDeserializer {
    private final ThreadLocal<Kryo> kryoThreadLocal;

    public KryoSerDeserializer(Kryo kryo) {
        kryoThreadLocal = ThreadLocal.withInitial(() -> kryo);
    }

    public KryoSerDeserializer() {
        this(Kryo.class.getClassLoader());
    }

    public KryoSerDeserializer(ClassLoader classLoader) {
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setRegistrationRequired(false);
//        kryo.setReferences(true);
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        } else {
            kryo.setClassLoader(ClassLoader.getPlatformClassLoader());
        }
        kryoThreadLocal = ThreadLocal.withInitial(() -> kryo);
    }

    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws Exception {
        if (obj == null)
            return null;
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setRegistrationRequired(false);
//        kryo.setReferences(true);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeObject(output, obj);
        output.close();

        Class<?> resultClass = Class.forName(obj.getClass().getName(), true, classLoader);
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        } else {
            kryo.setClassLoader(ClassLoader.getPlatformClassLoader());
        }
        Input input = new Input(bos.toByteArray());
        Object result = kryo.readObject(input, resultClass);
        input.close();

        return result;
    }

    @Override
    public <T> T deserialization(byte[] bytes, Class<T> type) {
        Kryo kryo = kryoThreadLocal.get();
        if (bytes == null)
            return null;
        kryo.register(type);
        Input input = new Input(bytes);
        T result = kryo.readObject(input, type);
        input.close();
        return result;
    }

    @Override
    public byte[] serialization(Object obj) {
        Kryo kryo = kryoThreadLocal.get();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeObject(output, obj);
        output.close();
        return bos.toByteArray();
    }
}

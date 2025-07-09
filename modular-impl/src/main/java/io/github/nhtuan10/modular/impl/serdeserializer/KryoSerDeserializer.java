package io.github.nhtuan10.modular.impl.serdeserializer;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;
import com.esotericsoftware.kryo.kryo5.objenesis.strategy.StdInstantiatorStrategy;

import java.io.ByteArrayOutputStream;

public class KryoSerDeserializer implements SerDeserializer {
    ThreadLocal<Kryo> kryoThreadLocal = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        kryo.setInstantiatorStrategy(new StdInstantiatorStrategy());
        kryo.setRegistrationRequired(false);
        kryo.setReferences(true);
        return kryo;
    });

    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws Exception {
        if (obj == null)
            return null;
        Kryo kryo = kryoThreadLocal.get();
        kryo.register(obj.getClass());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeObject(output, obj);
        output.close();

        Class<?> resultClass = Class.forName(obj.getClass().getName(), true, classLoader);
        if (classLoader != null) {
            kryo.setClassLoader(classLoader);
        }
        Input input = new Input(bos.toByteArray());
        Object result = kryo.readObject(input, resultClass);
        input.close();
        kryo.register(resultClass);
        return result;
    }
}

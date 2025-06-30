package io.github.nhtuan10.modular.serdeserializer;

import com.esotericsoftware.kryo.kryo5.Kryo;
import com.esotericsoftware.kryo.kryo5.io.Input;
import com.esotericsoftware.kryo.kryo5.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class KryoSerDeserializer implements SerDeserializer {
    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws Exception {
        if (obj == null)
            return null;
//        ClassLoader inClassLoader = obj.getClass().getClassLoader() != null ? obj.getClass().getClassLoader(): ClassLoader.getPlatformClassLoader();
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(false);
//        kryo.setClassLoader(inClassLoader);
//        kryo.register(obj.getClass());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        Output output = new Output(bos);
        kryo.writeObject(output, obj);
        output.close();

        Class<?> resultClass = Class.forName(obj.getClass().getName(), true, classLoader);
//        kryo.register(resultClass);
        kryo.setClassLoader(classLoader);
        Input input = new Input(new ByteArrayInputStream(bos.toByteArray()));
        Object result = kryo.readObject(input, resultClass);
        input.close();

        return result;
    }
}

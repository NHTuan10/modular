package io.github.nhtuan10.modular.impl.serdeserializer;


public interface SerDeserializer {
    Object castWithSerialization(Object obj, ClassLoader classLoader) throws Exception;

    <T> T deserialization(byte[] bytes, Class<T> type) throws Exception;

    byte[] serialization(Object obj);

    Module getJpmsModule();
}

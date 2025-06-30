package io.github.nhtuan10.modular.serdeserializer;


public interface SerDeserializer {
    Object castWithSerialization(Object obj, Class<?> clazz) throws Exception;
}

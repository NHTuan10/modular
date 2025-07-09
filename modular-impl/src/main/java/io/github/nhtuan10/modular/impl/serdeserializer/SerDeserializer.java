package io.github.nhtuan10.modular.impl.serdeserializer;


public interface SerDeserializer {
    Object castWithSerialization(Object obj, ClassLoader classLoader) throws Exception;
}

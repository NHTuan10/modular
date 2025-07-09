package io.github.nhtuan10.modular.impl.serdeserializer;

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;

import java.io.IOException;

/**
 * Currently not support collection class with generic type,
 * such as List<T>, it returns a map rather than generic type T during deserialization because of type erasure.
 * If this issue can't be fixed, this class will be planned for removal
 */
public class JacksonSmileSerDeserializer implements SerDeserializer {
    private final SmileMapper mapper = new SmileMapper();

    @Override
    public Object castWithSerialization(Object obj, ClassLoader classLoader) throws IOException, ClassNotFoundException {
        byte[] smileData = mapper.writeValueAsBytes(obj);
        return mapper.readValue(smileData, Class.forName(obj.getClass().getName(), true ,classLoader));
    }
}

package io.github.nhtuan10.modular.serdeserializer;

import com.fasterxml.jackson.dataformat.smile.databind.SmileMapper;

import java.io.IOException;

public class JacksonSmileSerDeserializer implements SerDeserializer {
    private final SmileMapper mapper = new SmileMapper();

    @Override
    public Object castWithSerialization(Object obj, Class<?> clazz) throws IOException {
        byte[] smileData = mapper.writeValueAsBytes(obj);
        return mapper.readValue(smileData, clazz);
    }
}

package io.quarkus.vault.client.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ObjectToStringSerializer extends JsonSerializer<Object> {
    @Override
    public void serialize(Object object, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (object == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(object);
            jsonGenerator.writeObject(jsonString);
        }
    }
}

package io.quarkus.vault.client.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.quarkus.vault.client.util.JsonMapping;

public class NumberToStringSerializer extends JsonSerializer<Number> {
    @Override
    public void serialize(Number number, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (number == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(number);
            jsonGenerator.writeObject(jsonString);
        }
    }
}

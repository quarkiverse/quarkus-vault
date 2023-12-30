package io.quarkus.vault.client.json;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ListToCommaStringSerializer extends JsonSerializer<List<String>> {
    @Override
    public void serialize(List<String> list, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (list == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writeObject(String.join(",", list));
        }
    }
}

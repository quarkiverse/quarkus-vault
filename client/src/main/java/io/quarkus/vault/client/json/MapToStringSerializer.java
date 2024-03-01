package io.quarkus.vault.client.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class MapToStringSerializer extends JsonSerializer<Map<String, Object>> {
    @Override
    public void serialize(Map<String, Object> map, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
            throws IOException {
        if (map == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(map);
            jsonGenerator.writeObject(jsonString);
        }
    }
}

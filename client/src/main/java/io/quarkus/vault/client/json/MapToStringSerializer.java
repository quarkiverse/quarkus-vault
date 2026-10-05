package io.quarkus.vault.client.json;

import java.util.Map;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class MapToStringSerializer extends ValueSerializer<Map<String, Object>> {
    @Override
    public void serialize(Map<String, Object> map, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        if (map == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(map);
            jsonGenerator.writePOJO(jsonString);
        }
    }
}

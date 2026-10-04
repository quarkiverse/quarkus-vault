package io.quarkus.vault.client.json;

import java.util.Map;

import tools.jackson.core.JsonParser;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StringToMapDeserializer extends ValueDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        String jsonString = jsonParser.getValueAsString();
        if (jsonString == null) {
            return null;
        }
        return JsonMapping.mapper.readValue(jsonString, new TypeReference<>() {
        });
    }
}

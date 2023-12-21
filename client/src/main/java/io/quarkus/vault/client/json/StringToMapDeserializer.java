package io.quarkus.vault.client.json;

import java.io.IOException;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import io.quarkus.vault.client.util.JsonMapping;

public class StringToMapDeserializer extends JsonDeserializer<Map<String, Object>> {
    @Override
    public Map<String, Object> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String jsonString = jsonParser.getValueAsString();
        if (jsonString == null) {
            return null;
        }
        return JsonMapping.mapper.readValue(jsonString, new TypeReference<>() {
        });
    }
}

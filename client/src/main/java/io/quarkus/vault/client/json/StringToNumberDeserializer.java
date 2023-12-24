package io.quarkus.vault.client.json;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class StringToNumberDeserializer extends JsonDeserializer<Number> {
    @Override
    public Number deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String jsonString = jsonParser.getValueAsString();
        if (jsonString == null) {
            return null;
        }
        return JsonMapping.mapper.readValue(jsonString, deserializationContext.getContextualType());
    }
}

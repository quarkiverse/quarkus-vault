package io.quarkus.vault.client.json;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class StringToNumberDeserializer extends ValueDeserializer<Number> {
    @Override
    public Number deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        String jsonString = jsonParser.getValueAsString();
        if (jsonString == null) {
            return null;
        }
        return JsonMapping.mapper.readValue(jsonString, deserializationContext.getContextualType());
    }
}

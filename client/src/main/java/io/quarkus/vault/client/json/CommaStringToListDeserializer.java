package io.quarkus.vault.client.json;

import java.util.Arrays;
import java.util.List;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

public class CommaStringToListDeserializer extends ValueDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) {
        String string = jsonParser.getValueAsString();
        if (string == null) {
            return null;
        }
        return Arrays.asList(string.split(","));
    }
}

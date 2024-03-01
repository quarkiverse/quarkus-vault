package io.quarkus.vault.client.json;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class CommaStringToListDeserializer extends JsonDeserializer<List<String>> {
    @Override
    public List<String> deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException {
        String string = jsonParser.getValueAsString();
        if (string == null) {
            return null;
        }
        return Arrays.asList(string.split(","));
    }
}

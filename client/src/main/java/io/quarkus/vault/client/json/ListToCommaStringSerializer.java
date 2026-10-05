package io.quarkus.vault.client.json;

import java.util.List;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class ListToCommaStringSerializer extends ValueSerializer<List<String>> {
    @Override
    public void serialize(List<String> list, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        if (list == null) {
            jsonGenerator.writeNull();
        } else {
            jsonGenerator.writePOJO(String.join(",", list));
        }
    }
}

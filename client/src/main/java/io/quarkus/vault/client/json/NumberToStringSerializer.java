package io.quarkus.vault.client.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class NumberToStringSerializer extends ValueSerializer<Number> {
    @Override
    public void serialize(Number number, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        if (number == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(number);
            jsonGenerator.writePOJO(jsonString);
        }
    }
}

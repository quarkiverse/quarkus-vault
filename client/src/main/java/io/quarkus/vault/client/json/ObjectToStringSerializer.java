package io.quarkus.vault.client.json;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

public class ObjectToStringSerializer extends ValueSerializer<Object> {
    @Override
    public void serialize(Object object, JsonGenerator jsonGenerator, SerializationContext serializerProvider) {
        if (object == null) {
            jsonGenerator.writeNull();
        } else {
            String jsonString = JsonMapping.mapper.writeValueAsString(object);
            jsonGenerator.writePOJO(jsonString);
        }
    }
}

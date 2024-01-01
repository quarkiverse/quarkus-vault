package io.quarkus.vault.client.json;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

public class JsonMapping {

    public static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .registerModule(new Jdk8Module());

    static {
        mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(VaultModule.INSTANCE);
    }

    public static <T> T convert(Object data, Class<T> type) {
        try {
            return JsonMapping.mapper.convertValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException("Error converting unwrapped result to expected type: " + type, e);
        }
    }

}

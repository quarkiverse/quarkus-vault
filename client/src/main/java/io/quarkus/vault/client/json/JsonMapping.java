package io.quarkus.vault.client.json;

import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

public class JsonMapping {

    public static final ObjectMapper mapper = JsonMapper.builder()
            .changeDefaultPropertyInclusion(incl -> incl.withContentInclusion(JsonInclude.Include.NON_NULL)
                    .withValueInclusion(JsonInclude.Include.NON_NULL))
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .addModule(VaultModule.INSTANCE)
            .build();

    public static <T> T convert(Object data, Class<T> type) {
        try {
            return JsonMapping.mapper.convertValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException("Error converting unwrapped result to expected type: " + type, e);
        }
    }

    public static <T> T convert(Object data, TypeReference<T> type) {
        try {
            return JsonMapping.mapper.convertValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException("Error converting unwrapped result to expected type: " + type, e);
        }
    }

}

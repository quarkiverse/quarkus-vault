package io.quarkus.vault.client.common;

import java.io.IOException;
import java.util.List;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.json.JsonMapping;

public class VaultJSONResultExtractor<T> implements VaultResultExtractor<T> {

    protected final Class<T> resultClass;

    public VaultJSONResultExtractor(Class<T> resultClass) {
        this.resultClass = resultClass;
    }

    public static <T> VaultJSONResultExtractor<T> of(Class<T> resultClass) {
        return new VaultJSONResultExtractor<>(resultClass);
    }

    @Override
    public T extract(VaultResponse<T> response) {
        return extract(response, resultClass);
    }

    public static <T> T extract(VaultResponse<T> response, Class<T> resultClass) {
        if (response.body == null || response.body.length == 0) {
            return null;
        }
        var request = response.request;
        try {
            return JsonMapping.mapper.readValue(response.body, resultClass);
        } catch (IOException e) {
            List<String> errors;
            if (e.getMessage() != null && !e.getMessage().isBlank()) {
                errors = List.of("Failed to parse response body", e.getMessage());
            } else {
                errors = List.of("Failed to parse response body");
            }
            throw new VaultClientException(request.getOperation(), request.getPath(), response.statusCode, errors);
        }
    }
}

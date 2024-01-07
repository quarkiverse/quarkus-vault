package io.quarkus.vault.client.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.common.VaultErrorResponse;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.json.JsonMapping;
import io.smallrye.mutiny.Uni;

public abstract class VaultHttpClient implements VaultRequestExecutor, AutoCloseable {

    public static final String X_VAULT_TOKEN = "X-Vault-Token";
    public static final String X_VAULT_WRAP_TTL = "X-Vault-Wrap-TTL";
    public static final String X_VAULT_NAMESPACE = "X-Vault-Namespace";

    protected <T> Uni<VaultResponse<T>> buildResponse(VaultRequest<T> request, int statusCode,
            Collection<Map.Entry<String, String>> headers, byte[] body) {
        return Uni.createFrom().nullItem()
                .map(none -> {
                    var response = new VaultResponse<>(request, statusCode, List.copyOf(headers), body);

                    if (!response.isStatusCodeExpected() && !response.isUpgradedResponse()) {
                        throwVaultException(request, statusCode, body);
                    }

                    return response;
                })
                .onFailure(JsonProcessingException.class)
                .transform(x -> new VaultClientException(request, statusCode, List.of("Failed to parse response body"), x))
                .onFailure(io.smallrye.mutiny.TimeoutException.class)
                .transform(x -> new VaultClientException(request, statusCode, List.of("Request timed out"), x))
                .onFailure(CompletionException.class).transform(e -> {
                    if (e.getCause() instanceof ConnectException) {
                        // unable to establish connection
                        return new VaultClientException(request, statusCode, List.of("Unable to establish connection"), e);
                    } else if (e.getCause() instanceof TimeoutException) {
                        // timeout on request - see HttpRequest.timeout(long)
                        return new VaultClientException(request, statusCode, List.of("HTTP request timed out"), e);
                    } else {
                        return e;
                    }
                });
    }

    private void throwVaultException(VaultRequest<?> request, int statusCode, byte[] body) {
        String bodyText = null;
        List<String> errors = null;
        try {
            VaultErrorResponse errorResponse = null;
            try {
                errorResponse = JsonMapping.mapper.readValue(body, VaultErrorResponse.class);
            } catch (Exception e) {
                // ignore
            }

            if (errorResponse != null && errorResponse.errors() != null && !errorResponse.errors().isEmpty()) {
                errors = errorResponse.errors();
            } else {
                bodyText = new String(body, UTF_8).trim();
            }
        } catch (Exception e) {
            // ignore
        }
        if (errors != null) {
            throw new VaultClientException(request, statusCode, errors, null);
        } else {
            throw new VaultClientException(request, statusCode, bodyText, null);
        }
    }

}

package io.quarkus.vault.client.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.ConnectException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.common.VaultErrorResponse;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.json.JsonMapping;

public abstract class VaultHttpClient implements VaultRequestExecutor, AutoCloseable {

    public static final String X_VAULT_TOKEN = "X-Vault-Token";
    public static final String X_VAULT_WRAP_TTL = "X-Vault-Wrap-TTL";
    public static final String X_VAULT_NAMESPACE = "X-Vault-Namespace";

    protected <T> CompletionStage<VaultResponse<T>> buildResponse(VaultRequest<T> request, int statusCode,
            Collection<Map.Entry<String, String>> headers, byte[] body) {
        return CompletableFuture.completedStage(null)
                .thenApply(none -> {
                    var response = new VaultResponse<>(request, statusCode, List.copyOf(headers), body);

                    if (!response.isStatusCodeExpected() && !response.isUpgradedResponse()) {
                        throwVaultException(request, statusCode, body);
                    }

                    return response;
                })
                .exceptionallyCompose(x -> {
                    // Unwrap CompletionException
                    if (x instanceof CompletionException || x instanceof ExecutionException) {
                        x = x.getCause();
                    }

                    if (x instanceof JsonProcessingException) {
                        x = new VaultClientException(request, statusCode, List.of("Failed to parse response body"), x);
                    } else if (x instanceof ConnectException) {
                        // unable to establish connection
                        x = new VaultClientException(request, statusCode, List.of("Unable to establish connection"), x);
                    } else if (x instanceof TimeoutException) {
                        // timeout on request - see HttpRequest.timeout(long)
                        x = new VaultClientException(request, statusCode, List.of("HTTP request timed out"), x);
                    }

                    return CompletableFuture.failedStage(x);
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

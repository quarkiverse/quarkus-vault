package io.quarkus.vault.client.http;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.VaultIOException;
import io.quarkus.vault.client.api.common.VaultLeasedResult;
import io.quarkus.vault.client.common.*;
import io.quarkus.vault.client.util.JsonMapping;
import io.smallrye.mutiny.Uni;

public abstract class VaultHttpClient implements VaultRequestExecutor, AutoCloseable {

    private static final Logger log = Logger.getLogger(VaultHttpClient.class.getName());

    protected String X_VAULT_TOKEN = "X-Vault-Token";
    protected String X_VAULT_WRAP_TTL = "X-Vault-Wrap-TTL";
    protected String X_VAULT_NAMESPACE = "X-Vault-Namespace";

    protected <T> byte[] requestBody(VaultRequest<T> request) {
        try {
            return JsonMapping.mapper.writeValueAsBytes(request.getBody());
        } catch (IOException e) {
            throw new VaultException("Failed to serialize request body", e);
        }
    }

    protected <T> Uni<T> processResponse(VaultRequest<T> request, int statusCode, byte[] body) {
        return Uni.createFrom().nullItem()
                .map((none) -> {

                    if (request.getMethod().equals(VaultRequest.Method.HEAD) && request.isStatusResult()) {
                        return request.getResultClass().cast(new VaultStatusResult(statusCode));
                    }

                    if (!request.getExpectedStatusCodes().contains(statusCode) && request.isLeasedResult()) {
                        if (!isWarningResponse(statusCode, request.getExpectedStatusCodes())) {
                            throwVaultException(request, statusCode, body);
                        }
                    }

                    var resultClass = request.getResultClass();
                    if (request.isVoidResult()) {
                        return null;
                    } else if (request.isStatusResult()) {
                        log.warning("Status result requested with required status: " + request.getOperation());
                        return resultClass.cast(new VaultStatusResult(statusCode));
                    } else if (request.isBinaryResult()) {
                        return resultClass.cast(new VaultBinaryResult(body));
                    } else if (request.isJSONResult()) {
                        var result = convert(body, resultClass);
                        if (result instanceof VaultLeasedResult) {
                            logResultWarnings((VaultLeasedResult<?, ?>) result);
                        }
                        return result;
                    } else {
                        throw new VaultClientException(request.getOperation(), request.getUrl().toString(), statusCode,
                                "Unsupported result class: " + resultClass);
                    }
                })
                .onFailure(JsonProcessingException.class).transform(VaultException::new)
                .onFailure(io.smallrye.mutiny.TimeoutException.class).transform(VaultIOException::new)
                .onFailure(CompletionException.class).transform(e -> {
                    if (e.getCause() instanceof ConnectException) {
                        // unable to establish connection
                        return new VaultIOException(e);
                    } else if (e.getCause() instanceof TimeoutException) {
                        // timeout on request - see HttpRequest.timeout(long)
                        return new VaultIOException(e);
                    } else {
                        return e;
                    }
                });
    }

    private boolean isWarningResponse(int statusCode, List<Integer> expectedCodes) {
        return expectedCodes.contains(204) && statusCode == 200;
    }

    private void logResultWarnings(VaultLeasedResult<?, ?> result) {
        if (result.warnings != null) {
            for (var warning : result.warnings) {
                log.warning(warning);
            }
        }
    }

    private void throwVaultException(VaultRequest<?> request, int statusCode, byte[] body) {
        String bodyText = null;
        List<String> errors = null;
        try {
            VaultErrorResponse errorResponse = null;
            try {
                errorResponse = convert(body, VaultErrorResponse.class);
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
            throw new VaultClientException(request.getOperation(), request.getUrl().toString(), statusCode, errors);
        } else {
            throw new VaultClientException(request.getOperation(), request.getUrl().toString(), statusCode, bodyText);
        }
    }

    private <T> T convert(byte[] data, Class<T> resultClass) {
        try {
            return JsonMapping.mapper.readValue(data, resultClass);
        } catch (IOException e) {
            throw new VaultException("Failed to parse Vault response", e);
        }
    }

}

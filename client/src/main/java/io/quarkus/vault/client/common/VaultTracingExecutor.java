package io.quarkus.vault.client.common;

import static java.lang.System.lineSeparator;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.logging.Level;
import java.util.logging.Logger;

public class VaultTracingExecutor implements VaultRequestExecutor {

    private static final Logger log = Logger.getLogger(VaultTracingExecutor.class.getName());

    private final VaultRequestExecutor delegate;

    public VaultTracingExecutor(VaultRequestExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
        log.info("REQUEST: " + request.getOperation() + lineSeparator() + getCurlFormattedRequest(request));
        return delegate.execute(request)
                .thenApply((response) -> {
                    var message = "RESPONSE: " + request.getOperation() + lineSeparator() + getHTTPFormattedResponse(response)
                            + lineSeparator();
                    log.log(response.isSuccessful() ? Level.INFO : Level.WARNING, message);
                    return response;
                })
                .exceptionallyCompose(error -> {
                    log.log(Level.SEVERE, "EXCEPTION: " + error);
                    return CompletableFuture.failedStage(error);
                });
    }

    private String getCurlFormattedRequest(VaultRequest<?> request) {
        var builder = new StringBuilder();
        builder.append("curl -X ").append(request.getMethod()).append(" \\").append(lineSeparator());
        request.getHTTPHeaders()
                .forEach((key, value) -> builder.append("  -H \"").append(key).append(": ").append(value).append("\" \\")
                        .append(lineSeparator()));
        request.getSerializedBody()
                .ifPresent(body -> builder.append("  -d '").append(body).append("' \\").append(lineSeparator()));
        builder.append("  ").append(request.getUrl());
        return builder.toString();
    }

    private String getHTTPFormattedResponse(VaultResponse<?> response) {
        var builder = new StringBuilder();
        builder.append("HTTP/1.1 ").append(response.getStatusCode()).append(lineSeparator());
        response.getHeaders()
                .forEach((entry) -> builder.append(entry.getKey()).append(": ").append(entry.getValue())
                        .append(lineSeparator()));
        builder.append(lineSeparator());
        response.getBodyAsString().ifPresent(builder::append);
        return builder.toString();
    }
}

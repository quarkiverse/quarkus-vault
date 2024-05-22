package io.quarkus.vault.client.http.jdk;

import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.http.VaultHttpClient;

public class JDKVaultHttpClient extends VaultHttpClient {

    private final HttpClient httpClient;

    public JDKVaultHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
        return CompletableFuture.completedStage(request)
                .thenApply(this::buildHTTPRequest)
                .thenCompose((httpRequest) -> httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray()))
                .exceptionallyCompose(JDKVaultHttpClient::mapError)
                .thenCompose(res -> buildResponse(request, res.statusCode(), headers(res), res.body()));
    }

    private HttpRequest buildHTTPRequest(VaultRequest<?> request) {

        var requestBuilder = HttpRequest.newBuilder()
                .uri(request.getUri())
                .timeout(request.getTimeout());

        request.getHTTPHeaders().forEach(requestBuilder::header);

        var body = request.getSerializedBody()
                .map(BodyPublishers::ofString)
                .orElseGet(BodyPublishers::noBody);

        return requestBuilder
                .method(request.getMethod().name(), body)
                .build();
    }

    private static <T> CompletionStage<HttpResponse<T>> mapError(Throwable x) {
        if (x instanceof HttpConnectTimeoutException) {
            return CompletableFuture.failedStage(new TimeoutException("HTTP connect time out: " + x.getMessage()));
        } else if (x instanceof HttpTimeoutException) {
            return CompletableFuture.failedStage(new TimeoutException("HTTP request time out: " + x.getMessage()));
        } else if (x instanceof CompletionException) {
            Throwable cause = x.getCause();
            if (cause instanceof HttpConnectTimeoutException) {
                return CompletableFuture.failedStage(new TimeoutException("HTTP connect time out: " + cause.getMessage()));
            } else if (cause instanceof HttpTimeoutException) {
                return CompletableFuture.failedStage(new TimeoutException("HTTP request time out: " + cause.getMessage()));
            }
        }
        return CompletableFuture.failedStage(x);
    }

    private static List<Map.Entry<String, String>> headers(HttpResponse<?> response) {
        var headers = new ArrayList<Map.Entry<String, String>>();
        response.headers().map().forEach((key, values) -> {
            values.forEach(value -> headers.add(Map.entry(key, value)));
        });
        return headers;
    }

    @Override
    public void close() {
    }
}

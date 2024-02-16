package io.quarkus.vault.client.http.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;

public class VertxVaultHttpClient extends VaultHttpClient {

    private static final HttpMethod LIST = HttpMethod.valueOf("LIST");

    private final WebClient webClient;

    public VertxVaultHttpClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
        var requestOptions = requestOptions(request);
        var httpRequest = webClient.request(httpMethodFor(request), requestOptions);
        return send(request, httpRequest)
                .thenCompose(res -> buildResponse(request, res));
    }

    private RequestOptions requestOptions(VaultRequest<?> request) {
        var options = new RequestOptions()
                .setTraceOperation(request.getOperation())
                .setAbsoluteURI(request.getUrl())
                .setTimeout(request.getTimeout().toMillis());
        request.getHTTPHeaders().forEach(options::addHeader);
        return options;
    }

    private CompletionStage<HttpResponse<Buffer>> send(VaultRequest<?> request, HttpRequest<Buffer> httpRequest) {

        var send = request.getSerializedBody()
                .map(Buffer::buffer)
                .map(httpRequest::sendBuffer)
                .orElseGet(httpRequest::send);

        return send.toCompletionStage()
                .exceptionallyCompose(e -> {
                    if (e instanceof CompletionException || e instanceof ExecutionException) {
                        e = e.getCause();
                    }

                    if ("Connection was closed".equals(e.getMessage())) {
                        // happens if the connection gets closed (idle timeout, reset by peer, ...)
                        e = new VaultClientException(request, null, List.of("Connection was closed"), e);
                    }

                    return CompletableFuture.failedStage(e);
                });
    }

    private <T> CompletionStage<VaultResponse<T>> buildResponse(VaultRequest<T> request, HttpResponse<Buffer> res) {
        var body = res.body();
        var bodyData = body != null ? body.getBytes() : null;
        return buildResponse(request, res.statusCode(), headers(res), bodyData);
    }

    private static HttpMethod httpMethodFor(VaultRequest<?> request) {
        return switch (request.getMethod()) {
            case LIST -> LIST;
            case GET -> HttpMethod.GET;
            case PUT -> HttpMethod.PUT;
            case POST -> HttpMethod.POST;
            case PATCH -> HttpMethod.PATCH;
            case DELETE -> HttpMethod.DELETE;
            case HEAD -> HttpMethod.HEAD;
        };
    }

    private static List<Map.Entry<String, String>> headers(HttpResponse<?> response) {
        return response.headers().entries();
    }

    public void close() {
        webClient.close();
    }

}

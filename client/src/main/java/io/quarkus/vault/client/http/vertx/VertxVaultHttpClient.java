package io.quarkus.vault.client.http.vertx;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.quarkus.vault.client.VaultIOException;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.smallrye.mutiny.Uni;
import io.vertx.core.VertxException;
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
    public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {
        var requestOptions = requestOptions(request);
        var httpRequest = webClient.request(httpMethodFor(request), requestOptions);
        return send(request, httpRequest)
                .flatMap(res -> buildResponse(request, res.statusCode(), headers(res), res.body().getBytes()));
    }

    private RequestOptions requestOptions(VaultRequest<?> request) {
        var options = new RequestOptions()
                .setTraceOperation(request.getOperation())
                .setAbsoluteURI(request.getUrl());

        request.getHeaders().forEach(options::addHeader);
        request.getNamespace().ifPresent(namespace -> options.addHeader(X_VAULT_NAMESPACE, namespace));
        request.getWrapTTLHeaderValue().ifPresent(wrapTTL -> options.addHeader(X_VAULT_WRAP_TTL, wrapTTL));
        request.getToken().ifPresent(token -> options.addHeader(X_VAULT_TOKEN, token));

        return options;
    }

    private Uni<HttpResponse<Buffer>> send(VaultRequest<?> request, HttpRequest<Buffer> httpRequest) {

        var send = request.getSerializedBody()
                .map(Buffer::buffer)
                .map(httpRequest::sendBuffer)
                .orElseGet(httpRequest::send)
                .toCompletionStage();

        return Uni.createFrom().completionStage(send)
                .ifNoItem().after(request.getTimeout()).failWith(TimeoutException::new)
                .onFailure(VertxException.class).transform(e -> {
                    if ("Connection was closed".equals(e.getMessage())) {
                        // happens if the connection gets closed (idle timeout, reset by peer, ...)
                        return new VaultIOException(e);
                    } else {
                        return e;
                    }
                });
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

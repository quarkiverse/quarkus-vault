package io.quarkus.vault.client.http.vertx;

import java.util.concurrent.TimeoutException;

import io.quarkus.vault.client.VaultIOException;
import io.quarkus.vault.client.common.VaultRequest;
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
    public <T> Uni<T> execute(VaultRequest<T> request) {
        var requestOptions = requestOptions(request);
        var httpRequest = webClient.request(httpMethodFor(request), requestOptions);
        return send(request, httpRequest)
                .flatMap(response -> processResponse(request, response.statusCode(), response.body().getBytes())
                        .onFailure(VertxException.class).transform(e -> {
                            if ("Connection was closed".equals(e.getMessage())) {
                                // happens if the connection gets closed (idle timeout, reset by peer, ...)
                                return new VaultIOException(e);
                            } else {
                                return e;
                            }
                        }));
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

    private RequestOptions requestOptions(VaultRequest<?> request) {
        var options = new RequestOptions()
                .setTraceOperation(request.getOperation())
                .setAbsoluteURI(request.getUrl());

        request.getHeaders().forEach(options::addHeader);

        if (request.getToken().isPresent()) {
            options.addHeader(X_VAULT_TOKEN, request.getToken().get());
        }

        if (request.getWrapTTL().isPresent()) {
            options.addHeader(X_VAULT_WRAP_TTL, String.valueOf(request.getWrapTTL().get().toSeconds()));
        }

        return options;
    }

    private Uni<HttpResponse<Buffer>> send(VaultRequest<?> request, HttpRequest<Buffer> httpRequest) {
        Uni<HttpResponse<Buffer>> send;
        if (request.getBody().isPresent()) {
            send = Uni.createFrom()
                    .completionStage(httpRequest.sendBuffer(Buffer.buffer(requestBody(request)))
                            .toCompletionStage());
        } else {
            send = Uni.createFrom().completionStage(httpRequest.send().toCompletionStage());
        }
        return send.ifNoItem().after(request.getTimeout()).failWith(TimeoutException::new);
    }

    public void close() {
        webClient.close();
    }

}

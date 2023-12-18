package io.quarkus.vault.client.http.jdk;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeoutException;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.quarkus.vault.client.util.JsonMapping;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

public class JDKVaultHttpClient extends VaultHttpClient {

    private final HttpClient httpClient;

    public JDKVaultHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public <T> Uni<T> execute(VaultRequest<T> request) {
        return Uni.createFrom().item(request)
                .map(Unchecked.function(this::buildHTTPRequest))
                .flatMap((httpRequest) -> {
                    var sent = httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray());
                    return Uni.createFrom().completionStage(sent)
                            .ifNoItem().after(request.getTimeout()).failWith(TimeoutException::new);
                })
                .flatMap(response -> processResponse(request, response.statusCode(), response.body()));
    }

    private HttpRequest buildHTTPRequest(VaultRequest<?> request) throws Exception {

        HttpRequest.BodyPublisher bodyPublisher;
        if (request.getBody().isPresent()) {
            var json = JsonMapping.mapper.writeValueAsString(request.getBody());
            bodyPublisher = HttpRequest.BodyPublishers.ofString(json);
        } else {
            bodyPublisher = HttpRequest.BodyPublishers.noBody();
        }

        var requestBuilder = HttpRequest.newBuilder()
                .uri(request.getUri())
                .method(request.getMethod().name(), bodyPublisher);

        request.getHeaders().forEach(requestBuilder::header);

        if (request.getNamespace().isPresent()) {
            requestBuilder.header(X_VAULT_NAMESPACE, request.getNamespace().get());
        }
        if (request.getWrapTTL().isPresent()) {
            requestBuilder.header(X_VAULT_WRAP_TTL, String.valueOf(request.getWrapTTL().get().toSeconds()));
        }
        if (request.getToken().isPresent()) {
            requestBuilder.header(X_VAULT_TOKEN, request.getToken().get());
        }
        return requestBuilder.build();
    }

    @Override
    public void close() {
    }
}

package io.quarkus.vault.client.http.jdk;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.smallrye.mutiny.Uni;

public class JDKVaultHttpClient extends VaultHttpClient {

    private final HttpClient httpClient;

    public JDKVaultHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {
        return Uni.createFrom().item(request)
                .map(this::buildHTTPRequest)
                .flatMap((httpRequest) -> {
                    var sent = httpClient.sendAsync(httpRequest, BodyHandlers.ofByteArray());
                    return Uni.createFrom().completionStage(sent)
                            .ifNoItem().after(request.getTimeout()).failWith(TimeoutException::new);
                })
                .flatMap(res -> buildResponse(request, res.statusCode(), headers(res), res.body()));
    }

    private HttpRequest buildHTTPRequest(VaultRequest<?> request) {

        var requestBuilder = HttpRequest.newBuilder()
                .uri(request.getUri());

        request.getHeaders().forEach(requestBuilder::header);
        request.getNamespace().ifPresent(namespace -> requestBuilder.header(X_VAULT_NAMESPACE, namespace));
        request.getWrapTTLHeaderValue().ifPresent(wrapTTL -> requestBuilder.header(X_VAULT_WRAP_TTL, wrapTTL));
        request.getToken().ifPresent(token -> requestBuilder.header(X_VAULT_TOKEN, token));

        var body = request.getSerializedBody()
                .map(BodyPublishers::ofString)
                .orElseGet(BodyPublishers::noBody);

        return requestBuilder
                .method(request.getMethod().name(), body)
                .build();
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

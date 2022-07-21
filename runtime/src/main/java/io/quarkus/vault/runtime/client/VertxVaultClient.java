package io.quarkus.vault.runtime.client;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.Collections.emptyMap;

import java.net.ConnectException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultIOException;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;
import io.vertx.core.VertxException;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.RequestOptions;
import io.vertx.mutiny.core.buffer.Buffer;
import io.vertx.mutiny.ext.web.client.HttpRequest;
import io.vertx.mutiny.ext.web.client.HttpResponse;
import io.vertx.mutiny.ext.web.client.WebClient;

public abstract class VertxVaultClient implements VaultClient {

    private static final HttpMethod LIST = HttpMethod.valueOf("LIST");

    private static final List<String> ROOT_NAMESPACE_API = Arrays.asList("sys/init", "sys/license", "sys/leader", "sys/health",
            "sys/metrics", "sys/config/state", "sys/host-info", "sys/key-status", "sys/storage", "sys/storage/raft");

    private URL baseUrl;
    private Duration requestTimeout;
    private Optional<String> namespace;

    private ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    protected VertxVaultClient(URL baseUrl, Optional<String> namespace, Duration requestTimeout) {
        this.baseUrl = baseUrl;
        this.namespace = namespace;
        this.requestTimeout = requestTimeout;
        this.mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    protected abstract WebClient getWebClient();

    // ---

    public <T> Uni<T> put(String operation, String path, String token, Object body, int expectedCode) {
        HttpRequest<Buffer> request = builder(operation, HttpMethod.PUT, path, token);
        return exec(request, body, null, expectedCode);
    }

    public <T> Uni<T> list(String operationName, String path, String token, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(operationName, LIST, path, token);
        return exec(request, resultClass);
    }

    public <T> Uni<T> delete(String operationName, String path, String token, int expectedCode) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.DELETE, path, token);
        return exec(request, expectedCode);
    }

    public <T> Uni<T> post(String operationName, String path, String token, Object body, Class<T> resultClass,
            int expectedCode) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.POST, path, token);
        return exec(request, body, resultClass, expectedCode);
    }

    public <T> Uni<T> post(String operationName, String path, String token, Object body, Class<T> resultClass) {
        return post(operationName, path, token, emptyMap(), body, resultClass);
    }

    public <T> Uni<T> post(String operationName, String path, String token, Map<String, String> headers, Object body,
            Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.POST, path, token);
        headers.forEach(request::putHeader);
        return exec(request, body, resultClass);
    }

    public <T> Uni<T> post(String operationName, String path, String token, Object body, int expectedCode) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.POST, path, token);
        return exec(request, body, null, expectedCode);
    }

    public <T> Uni<T> put(String operationName, String path, String token, Object body, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.PUT, path, token);
        return exec(request, body, resultClass);
    }

    public <T> Uni<T> put(String operationName, String path, Object body, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.PUT, path);
        return exec(request, body, resultClass);
    }

    public <T> Uni<T> get(String operationName, String path, String token, Class<T> resultClass) {
        HttpRequest<Buffer> request = builder(operationName, HttpMethod.GET, path, token);
        return exec(request, resultClass);
    }

    public <T> Uni<T> get(String operationName, String path, Map<String, String> queryParams, Class<T> resultClass) {
        final HttpRequest<Buffer> request = builder(operationName, HttpMethod.GET, path, queryParams);
        return exec(request, resultClass);
    }

    public Uni<Buffer> get(String operationName, String path, String token) {
        final HttpRequest<Buffer> request = builder(operationName, HttpMethod.GET, path, token);
        return request.send().ifNoItem().after(getRequestTimeout())
                .fail().map(response -> {
                    if (response.statusCode() != 200 && response.statusCode() != 204) {
                        throwVaultException(response);
                    }
                    return response.body();
                });
    }

    public Uni<Integer> head(String operationName, String path) {
        final HttpRequest<Buffer> request = builder(operationName, HttpMethod.HEAD, path);
        return exec(request);
    }

    public Uni<Integer> head(String operationName, String path, Map<String, String> queryParams) {
        final HttpRequest<Buffer> request = builder(operationName, HttpMethod.HEAD, path, queryParams);
        return exec(request);
    }

    private <T> Uni<T> exec(HttpRequest<Buffer> request, Class<T> resultClass) {
        return exec(request, null, resultClass, 200);
    }

    private <T> Uni<T> exec(HttpRequest<Buffer> request, int expectedCode) {
        return exec(request, null, null, expectedCode);
    }

    private <T> Uni<T> exec(HttpRequest<Buffer> request, Object body, Class<T> resultClass) {
        return exec(request, body, resultClass, 200);
    }

    private <T> Uni<T> exec(HttpRequest<Buffer> request, Object body, Class<T> resultClass, int expectedCode) {
        Uni<HttpResponse<Buffer>> send = body == null ? request.send() : request.sendBuffer(Buffer.buffer(requestBody(body)));

        return send.ifNoItem().after(getRequestTimeout()).failWith(TimeoutException::new)
                .map(Unchecked.function(response -> {
                    if (response.statusCode() != expectedCode) {
                        throwVaultException(response);
                    }

                    Buffer responseBuffer = response.body();
                    if (responseBuffer != null) {
                        return resultClass == null ? null : mapper.readValue(responseBuffer.toString(), resultClass);
                    } else {
                        return null;
                    }
                }))
                .onFailure(JsonProcessingException.class).transform(VaultException::new)
                .onFailure(io.smallrye.mutiny.TimeoutException.class).transform(VaultIOException::new)
                .onFailure(VertxException.class).transform(e -> {
                    if ("Connection was closed".equals(e.getMessage())) {
                        // happens if the connection gets closed (idle timeout, reset by peer, ...)
                        return new VaultIOException(e);
                    } else {
                        return e;
                    }
                })
                .onFailure(CompletionException.class).transform(e -> {
                    if (e.getCause() instanceof ConnectException) {
                        // unable to establish connection
                        return new VaultIOException(e);
                    } else if (e.getCause() instanceof java.util.concurrent.TimeoutException) {
                        // timeout on request - see HttpRequest.timeout(long)
                        return new VaultIOException(e);
                    } else {
                        return e;
                    }
                });

    }

    private Duration getRequestTimeout() {
        return requestTimeout;
    }

    private Uni<Integer> exec(HttpRequest<Buffer> request) {
        return request.send()
                .ifNoItem().after(getRequestTimeout()).failWith(TimeoutException::new)
                .onItem().transform(HttpResponse::statusCode);
    }

    private void throwVaultException(HttpResponse<Buffer> response) {
        String body = null;
        try {
            body = response.body().toString();
        } catch (Exception e) {
            // ignore
        }
        throw new VaultClientException(response.statusCode(), body);
    }

    private HttpRequest<Buffer> builder(String operationName, HttpMethod method, String path, String token) {
        HttpRequest<Buffer> request = builder(operationName, method, path);
        if (token != null) {
            request.putHeader(X_VAULT_TOKEN, token);
        }
        if (namespace.isPresent() && !isRootNamespaceAPI(path)) {
            request.putHeader(X_VAULT_NAMESPACE, namespace.get());
        }
        return request;
    }

    private boolean isRootNamespaceAPI(String path) {
        return ROOT_NAMESPACE_API.stream().anyMatch(path::startsWith);
    }

    private HttpRequest<Buffer> builder(String operationName, HttpMethod method, String path) {
        RequestOptions options = new RequestOptions()
                .setAbsoluteURI(getUrl(path))
                .setTraceOperation(operationName);
        return getWebClient().request(method, options);
    }

    private HttpRequest<Buffer> builder(String operationName, HttpMethod method, String path, Map<String, String> queryParams) {
        HttpRequest<Buffer> request = builder(operationName, method, path);
        if (queryParams != null) {
            queryParams.forEach(request::addQueryParam);
        }
        return request;
    }

    private String requestBody(Object body) {
        try {
            return mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new VaultException(e);
        }
    }

    private URL getUrl(String path) {
        try {
            return new URL(baseUrl, API_VERSION + "/" + path);
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }
}

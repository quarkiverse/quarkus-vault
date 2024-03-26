package io.quarkus.vault.client.common;

import static io.quarkus.vault.client.http.VaultHttpClient.*;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.json.JsonMapping;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultRequest<T> {

    public enum Method {
        GET,
        PUT,
        POST,
        PATCH,
        DELETE,
        HEAD,
        LIST
    }

    public static List<Integer> OK_STATUS = List.of(200);
    public static List<Integer> NO_CONTENT_STATUS = List.of(204);
    public static List<Integer> ACCEPTED_STATUS = List.of(202);
    public static List<Integer> OK_OR_ACCEPTED_STATUS = List.of(200, 202);
    public static List<Integer> OK_OR_NO_CONTENT_STATUS = List.of(200, 204);

    public static class Builder<T> {
        private URL baseUrl;
        private String apiVersion = "v1";
        private String operation;
        private Method method;
        private String path;
        private Optional<String> token;
        private Optional<String> namespace;
        private Optional<Duration> wrapTTL;
        private Map<String, String> queryParams = new LinkedHashMap<>();
        private Map<String, String> headers = new LinkedHashMap<>();
        private Object body;
        private VaultResultExtractor<?> resultExtractor;
        private List<Integer> expectedStatusCodes = List.of();
        private Duration timeout = Duration.ofSeconds(30);
        private LogConfidentialityLevel logConfidentialityLevel = LogConfidentialityLevel.HIGH;

        private Builder(String operation, Method method) {
            this.operation = operation;
            this.method = method;
        }

        public Builder<T> baseUrl(URL baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder<T> apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder<T> path(Object... pathSegments) {
            this.path = joinPath(pathSegments);
            return this;
        }

        public final Builder<T> pathChoice(boolean selector, Object[] truePathSegments, Object[] falsePathSegments) {
            return path(selector ? truePathSegments : falsePathSegments);
        }

        public final Builder<T> pathChoice(Object selector, Map<Object, Object[]> options) {
            for (Map.Entry<Object, Object[]> option : options.entrySet()) {
                if (option.getKey().equals(selector)) {
                    return path(option.getValue());
                }
            }
            throw new IllegalArgumentException("No path choice for selector " + selector);
        }

        public Builder<T> token(String token) {
            this.token = Optional.ofNullable(token);
            return this;
        }

        public Builder<T> noToken() {
            return token(null);
        }

        public Builder<T> wrapTTL(Duration ttl) {
            this.wrapTTL = Optional.of(ttl);
            return this;
        }

        public Builder<T> namespace(String namespace) {
            this.namespace = Optional.of(namespace);
            return this;
        }

        public Builder<T> noNamespace() {
            this.namespace = Optional.empty();
            return this;
        }

        public Builder<T> queryParam(String key, Object value) {
            Objects.requireNonNull(key, "key is required");
            var valueStr = JsonMapping.mapper.convertValue(value, String.class);
            queryParams.put(key, valueStr);
            return this;
        }

        public Builder<T> queryParam(boolean condition, String key, Object value) {
            if (condition) {
                queryParam(key, value);
            }
            return this;
        }

        public Builder<T> header(String key, Object value) {
            Objects.requireNonNull(key, "key is required");
            Objects.requireNonNull(value, "value is required");
            var valueStr = JsonMapping.mapper.convertValue(value, String.class);
            this.headers.put(key, valueStr);
            return this;
        }

        public Builder<T> header(boolean condition, String key, Object value) {
            if (condition) {
                header(key, value);
            }
            return this;
        }

        public Builder<T> body(Object body) {
            this.body = body;
            return this;
        }

        public Builder<T> expectedStatusCodes(List<Integer> expectedStatusCodes) {
            this.expectedStatusCodes = expectedStatusCodes;
            return this;
        }

        public Builder<T> expectOkStatus() {
            return expectedStatusCodes(OK_STATUS);
        }

        public Builder<T> expectNoContentStatus() {
            return expectedStatusCodes(NO_CONTENT_STATUS);
        }

        public Builder<T> expectAcceptedStatus() {
            return expectedStatusCodes(ACCEPTED_STATUS);
        }

        public Builder<T> expectOkOrAcceptedStatus() {
            return expectedStatusCodes(OK_OR_ACCEPTED_STATUS);
        }

        public Builder<T> expectOkOrNoContentStatus() {
            return expectedStatusCodes(OK_OR_NO_CONTENT_STATUS);
        }

        public Builder<T> expectAnyStatus() {
            return expectedStatusCodes(List.of());
        }

        public Builder<T> timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder<T> logConfidentialityLevel(LogConfidentialityLevel logConfidentialityLevel) {
            this.logConfidentialityLevel = logConfidentialityLevel;
            return this;
        }

        public VaultRequest<T> rebuild() {
            return new VaultRequest<>(this);
        }

        public <U> VaultRequest<U> build(VaultResultExtractor<U> resultExtractor) {
            this.resultExtractor = resultExtractor;
            return new VaultRequest<>(this);
        }

        public VaultRequest<Void> build() {
            return build(VaultVoidResultExtractor.INSTANCE);
        }
    }

    private final URL baseUrl;
    private final String apiVersion;
    private final String operation;
    private final Method method;
    private final String path;
    private final Optional<String> token;
    private final Optional<String> namespace;
    private final Optional<Duration> wrapTTL;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Object body;
    private final VaultResultExtractor<T> resultExtractor;
    private final List<Integer> expectedStatusCodes;
    private final Duration timeout;
    private final LogConfidentialityLevel logConfidentialityLevel;

    @SuppressWarnings("unchecked")
    private VaultRequest(Builder<?> builder) {
        this.baseUrl = builder.baseUrl;
        this.apiVersion = builder.apiVersion;
        this.operation = builder.operation;
        this.method = builder.method;
        this.path = builder.path;
        this.token = builder.token;
        this.namespace = builder.namespace;
        this.wrapTTL = builder.wrapTTL;
        this.headers = builder.headers;
        this.queryParams = builder.queryParams;
        this.body = builder.body;
        this.expectedStatusCodes = builder.expectedStatusCodes;
        this.timeout = builder.timeout;
        this.logConfidentialityLevel = builder.logConfidentialityLevel;
        this.resultExtractor = (VaultResultExtractor<T>) builder.resultExtractor;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public String getOperation() {
        return operation;
    }

    public Method getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public boolean hasToken() {
        return token != null;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<String> getToken() {
        return token != null ? token : Optional.empty();
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public boolean hasNamespace() {
        return namespace != null;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<String> getNamespace() {
        return namespace != null ? namespace : Optional.empty();
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public boolean hasWrapTTL() {
        return wrapTTL != null;
    }

    @SuppressWarnings("OptionalAssignedToNull")
    public Optional<Duration> getWrapTTL() {
        return wrapTTL != null ? wrapTTL : Optional.empty();
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getQueryParams() {
        return queryParams;
    }

    public boolean hasBody() {
        return body != null;
    }

    public Optional<Object> getBody() {
        return Optional.ofNullable(body);
    }

    public Optional<String> getSerializedBody() {
        if (body == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(JsonMapping.mapper.writeValueAsString(body));
        } catch (Exception e) {
            throw new VaultClientException(this, null, List.of("Failed to serialize request body"), null);
        }
    }

    public VaultResultExtractor<T> getResultExtractor() {
        return resultExtractor;
    }

    public List<Integer> getExpectedStatusCodes() {
        return expectedStatusCodes;
    }

    public LogConfidentialityLevel getLogConfidentialityLevel() {
        return logConfidentialityLevel;
    }

    public URL getUrl() {
        if (baseUrl == null) {
            throw new IllegalStateException("baseUrl is not set");
        }

        var fullPath = joinPath(baseUrl.getPath(), apiVersion, path);
        if (!queryParams.isEmpty()) {
            fullPath += "?" + getQueryParamsString();
        }

        try {
            return new URL(baseUrl, fullPath);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid URL for Vault request", e);
        }
    }

    public Map<String, String> getHTTPHeaders() {
        var allHeaders = new HashMap<>(headers);
        getToken().ifPresent(token -> allHeaders.put(X_VAULT_TOKEN, token));
        getNamespace().ifPresent(namespace -> allHeaders.put(X_VAULT_NAMESPACE, namespace));
        getWrapTTL().ifPresent(wrapTTL -> allHeaders.put(X_VAULT_WRAP_TTL, String.valueOf(wrapTTL.toSeconds())));
        return allHeaders;
    }

    public URI getUri() {
        return URI.create(getUrl().toString());
    }

    private String getQueryParamsString() {
        return queryParams.entrySet().stream()
                .map(e -> {
                    var key = URLEncoder.encode(e.getKey(), UTF_8);
                    var value = e.getValue();
                    if (value == null) {
                        return key;
                    }
                    return key + "=" + URLEncoder.encode(value, UTF_8);
                })
                .collect(Collectors.joining("&"));
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Builder<T> builder() {
        var builder = new Builder<T>(operation, method);
        builder.baseUrl = baseUrl;
        builder.apiVersion = apiVersion;
        builder.operation = operation;
        builder.method = method;
        builder.path = path;
        builder.token = token;
        builder.namespace = namespace;
        builder.wrapTTL = wrapTTL;
        builder.queryParams = queryParams;
        builder.headers = headers;
        builder.body = body;
        builder.expectedStatusCodes = expectedStatusCodes;
        builder.timeout = timeout;
        builder.resultExtractor = resultExtractor;
        builder.logConfidentialityLevel = logConfidentialityLevel;
        return builder;
    }

    public static <T> Builder<T> request(String operation, Method method) {
        return new Builder<T>(operation, method);
    }

    public static <T> Builder<T> get(String operation) {
        return request(operation, Method.GET);
    }

    public static <T> Builder<T> post(String operation) {
        return request(operation, Method.POST);
    }

    public static <T> Builder<T> put(String operation) {
        return request(operation, Method.PUT);
    }

    public static <T> Builder<T> patch(String operation) {
        return request(operation, Method.PATCH);
    }

    public static <T> Builder<T> delete(String operation) {
        return request(operation, Method.DELETE);
    }

    public static <T> Builder<T> list(String operation) {
        return request(operation, Method.LIST);
    }

    public static <T> Builder<T> head(String operation) {
        return request(operation, Method.HEAD);
    }

    private static String joinPath(Object... pathSegments) {
        return Arrays.stream(pathSegments)
                .filter(Objects::nonNull)
                .map(s -> JsonMapping.mapper.convertValue(s, String.class))
                .map(s -> s.startsWith("/") ? s.substring(1) : s)
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }
}

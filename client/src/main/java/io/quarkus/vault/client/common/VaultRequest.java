package io.quarkus.vault.client.common;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import io.quarkus.vault.client.api.common.VaultLeasedResult;
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

    public static class Builder {
        private URL baseUrl;
        private String apiVersion = "v1";
        private String operation;
        private Method method;
        private String mountPath;
        private String path;
        private Optional<String> token;
        private Optional<String> namespace;
        private Optional<Duration> wrapTTL;
        private Map<String, String> queryParams = new HashMap<>();
        private Map<String, String> headers = new HashMap<>();
        private Object body;
        private List<Integer> expectedStatusCodes;
        private Class<?> resultClass;
        private Duration timeout = Duration.ofSeconds(30);
        private LogConfidentialityLevel logConfidentialityLevel = LogConfidentialityLevel.HIGH;

        private Builder(String operation, Method method, String mountPath) {
            this.operation = operation;
            this.method = method;
            this.mountPath = mountPath;
        }

        public Builder baseUrl(URL baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
            return this;
        }

        public Builder path(String... pathSegments) {
            this.path = joinPath(pathSegments);
            return this;
        }

        public final Builder pathChoice(boolean selector, String[] truePathSegments, String[] falsePathSegments) {
            return path(selector ? truePathSegments : falsePathSegments);
        }

        public final Builder pathChoice(Object selector, Map.Entry<Object, String[]>... options) {
            for (Map.Entry<Object, String[]> option : options) {
                if (option.getKey().equals(selector)) {
                    return path(option.getValue());
                }
            }
            throw new IllegalArgumentException("No path choice for selector " + selector);
        }

        public Builder token(String token) {
            this.token = Optional.ofNullable(token);
            return this;
        }

        public Builder noToken() {
            return token(null);
        }

        public Builder wrapTTL(Duration ttl) {
            this.wrapTTL = Optional.of(ttl);
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = Optional.of(namespace);
            return this;
        }

        public Builder noNamespace() {
            this.namespace = Optional.empty();
            return this;
        }

        public Builder queryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
            return this;
        }

        public Builder queryParam(boolean condition, String key, Object value) {
            if (condition) {
                this.queryParams.put(key, value.toString());
            }
            return this;
        }

        public Builder queryParam(String key, Object value) {
            return queryParam(true, key, value);
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Builder header(String key, Object value) {
            this.headers.put(key, value.toString());
            return this;
        }

        public Builder body(Object body) {
            this.body = body;
            return this;
        }

        public Builder expectedStatusCodes(List<Integer> expectedStatusCodes) {
            this.expectedStatusCodes = expectedStatusCodes;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public Builder logConfidentialityLevel(LogConfidentialityLevel logConfidentialityLevel) {
            this.logConfidentialityLevel = logConfidentialityLevel;
            return this;
        }

        public <T extends VaultResult> VaultRequest<T> build(Class<T> resultClass) {
            this.resultClass = resultClass;
            return new VaultRequest<>(this);
        }

        public <T> VaultRequest<T> build() {
            return new VaultRequest<>(this);
        }

        public <T extends VaultResult> VaultRequest<T> ok(Class<T> resultClass) {
            return expectedStatusCodes(OK_STATUS).build(resultClass);
        }

        public VaultRequest<Void> ok() {
            return expectedStatusCodes(OK_STATUS).build();
        }

        public VaultRequest<Void> noContent() {
            return expectedStatusCodes(NO_CONTENT_STATUS).build();
        }

        public <T extends VaultResult> VaultRequest<T> accepted(Class<T> resultClass) {
            return expectedStatusCodes(ACCEPTED_STATUS).build(resultClass);
        }

        public VaultRequest<Void> accepted() {
            return expectedStatusCodes(ACCEPTED_STATUS).build();
        }

        public <T extends VaultResult> VaultRequest<T> okOrAccepted(Class<T> resultClass) {
            return expectedStatusCodes(OK_OR_ACCEPTED_STATUS).build(resultClass);
        }

        public VaultRequest<Void> okOrAccepted() {
            return expectedStatusCodes(OK_OR_ACCEPTED_STATUS).build();
        }
    }

    private final URL baseUrl;
    private final String apiVersion;
    private final String operation;
    private final Method method;
    private final String mountPath;
    private final String path;
    private final Optional<String> token;
    private final Optional<String> namespace;
    private final Optional<Duration> wrapTTL;
    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Object body;
    private final Class<T> resultClass;
    private final List<Integer> expectedStatusCodes;
    private final Duration timeout;
    private final LogConfidentialityLevel logConfidentialityLevel;

    @SuppressWarnings("unchecked")
    private VaultRequest(Builder builder) {
        this.baseUrl = builder.baseUrl;
        this.apiVersion = builder.apiVersion;
        this.operation = builder.operation;
        this.method = builder.method;
        this.mountPath = builder.mountPath;
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
        this.resultClass = (Class<T>) builder.resultClass;
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

    public String getMountPath() {
        return mountPath;
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

    public Class<T> getResultClass() {
        return resultClass;
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

        var fullPath = joinPath(baseUrl.getPath(), apiVersion, mountPath, path);
        if (!queryParams.isEmpty()) {
            fullPath += "?" + getQueryParamsString();
        }

        try {
            return new URL(baseUrl, fullPath);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid URL for Vault request", e);
        }
    }

    public URI getUri() {
        return URI.create(getUrl().toString());
    }

    private String getQueryParamsString() {
        return queryParams.entrySet().stream()
                .map(e -> {
                    var key = URLEncoder.encode(e.getKey(), UTF_8);
                    var value = URLEncoder.encode(e.getValue(), UTF_8);
                    return key + "=" + value;
                })
                .collect(Collectors.joining("&"));
    }

    public boolean isVoidResult() {
        return Void.class == resultClass || void.class == resultClass;
    }

    public boolean isBinaryResult() {
        return VaultBinaryResult.class.isAssignableFrom(resultClass);
    }

    public boolean isJSONResult() {
        return VaultJSONResult.class.isAssignableFrom(resultClass);
    }

    public boolean isLeasedResult() {
        return VaultLeasedResult.class.isAssignableFrom(resultClass);
    }

    public boolean isStatusResult() {
        return VaultStatusResult.class.isAssignableFrom(resultClass);
    }

    public Duration getTimeout() {
        return timeout;
    }

    public Builder builder() {
        var builder = new Builder(operation, method, mountPath);
        builder.baseUrl = baseUrl;
        builder.apiVersion = apiVersion;
        builder.path = path;
        builder.token = token;
        builder.namespace = namespace;
        builder.wrapTTL = wrapTTL;
        builder.queryParams = queryParams;
        builder.headers = headers;
        builder.body = body;
        builder.expectedStatusCodes = expectedStatusCodes;
        builder.resultClass = resultClass;
        builder.logConfidentialityLevel = logConfidentialityLevel;
        return builder;
    }

    public static Builder request(String operation, Method method, String mountPath) {
        return new Builder(operation, method, mountPath);
    }

    public static Builder get(String operation, String mountPath) {
        return request(operation, Method.GET, mountPath);
    }

    public static Builder post(String operation, String mountPath) {
        return request(operation, Method.POST, mountPath);
    }

    public static Builder put(String operation, String mountPath) {
        return request(operation, Method.PUT, mountPath);
    }

    public static Builder patch(String operation, String mountPath) {
        return request(operation, Method.PATCH, mountPath);
    }

    public static Builder delete(String operation, String mountPath) {
        return request(operation, Method.DELETE, mountPath);
    }

    public static Builder list(String operation, String mountPath) {
        return request(operation, Method.LIST, mountPath);
    }

    public static Builder head(String operation, String mountPath) {
        return request(operation, Method.HEAD, mountPath);
    }

    private static String joinPath(String... pathSegments) {
        return Arrays.stream(pathSegments)
                .filter(Objects::nonNull)
                .map(s -> s.startsWith("/") ? s.substring(1) : s)
                .map(s -> s.endsWith("/") ? s.substring(0, s.length() - 1) : s)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.joining("/"));
    }
}

package io.quarkus.vault.client;

import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.time.InstantSource;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.vault.client.api.VaultAuthAccessor;
import io.quarkus.vault.client.api.VaultSecretsAccessor;
import io.quarkus.vault.client.api.VaultSysAccessor;
import io.quarkus.vault.client.auth.*;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.common.VaultTracingExecutor;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.operators.uni.UniRetryAtMost;

public class VaultClient implements VaultRequestExecutor {

    public static class Builder {

        private URL baseUrl;
        private String apiVersion;
        private VaultRequestExecutor executor;
        private VaultTokenProvider tokenProvider;
        private String namespace;
        private Duration requestTimeout;
        private LogConfidentialityLevel logConfidentialityLevel;
        private InstantSource instantSource = InstantSource.system();

        public Builder baseUrl(URL baseUrl) {
            this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl is required");
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            try {
                return baseUrl(new URL(baseUrl));
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid URL: " + baseUrl, e);
            }
        }

        public Builder apiVersion(String apiVersion) {
            this.apiVersion = Objects.requireNonNull(apiVersion, "apiVersion is required");
            assert apiVersion.startsWith("v");
            return this;
        }

        public Builder executor(VaultRequestExecutor executor) {
            this.executor = executor;
            return this;
        }

        public Builder clientToken(String token) {
            return clientToken(VaultStaticClientTokenAuthOptions.builder().token(token).build());
        }

        public Builder clientToken(VaultStaticClientTokenAuthOptions options) {
            return tokenProvider(new VaultStaticClientTokenProvider(options));
        }

        public Builder userPass(String username, String password) {
            return userPass(VaultUserPassAuthOptions.builder()
                    .username(username)
                    .password(password)
                    .build());
        }

        public Builder userPass(VaultUserPassAuthOptions options) {
            return tokenProvider(new VaultUserPassTokenProvider(options).caching(options.cachingRenewGracePeriod));
        }

        public Builder appRole(String roleId, String secretId) {
            return appRole(VaultAppRoleAuthOptions.builder()
                    .roleId(roleId)
                    .secretId(secretId)
                    .build());
        }

        public Builder appRole(VaultAppRoleAuthOptions options) {
            return tokenProvider(new VaultAppRoleTokenProvider(options).caching(options.cachingRenewGracePeriod));
        }

        public Builder kubernetes(String role, Path jwtTokenPath) {
            return kubernetes(VaultKubernetesAuthOptions.builder()
                    .role(role)
                    .jwtTokenPath(jwtTokenPath)
                    .build());
        }

        public Builder kubernetes(VaultKubernetesAuthOptions options) {
            return tokenProvider(new VaultKubernetesTokenProvider(options).caching(options.cachingRenewGracePeriod));
        }

        public Builder tokenProvider(VaultTokenProvider tokenProvider) {
            this.tokenProvider = tokenProvider;
            return this;
        }

        public Builder namespace(String namespace) {
            this.namespace = namespace;
            return this;
        }

        public Builder requestTimeout(Duration requestTimeout) {
            this.requestTimeout = requestTimeout;
            return this;
        }

        public Builder logConfidentialityLevel(LogConfidentialityLevel logConfidentialityLevel) {
            this.logConfidentialityLevel = logConfidentialityLevel != null ? logConfidentialityLevel
                    : LogConfidentialityLevel.HIGH;
            return this;
        }

        public Builder traceRequests() {
            Objects.requireNonNull(executor, "executor must be configured before tracing");
            if (!(executor instanceof VaultTracingExecutor)) {
                executor = new VaultTracingExecutor(executor);
            }
            return this;
        }

        /**
         * Sets the {@link InstantSource} to use for time-based operations.
         * <p>
         * This is for testing.
         *
         * @param instantSource the instant source
         * @return this builder
         */
        public Builder instantSource(InstantSource instantSource) {
            this.instantSource = instantSource;
            return this;
        }

        public VaultClient build() {
            return new VaultClient(this);
        }

    }

    public static Builder builder() {
        return new Builder();
    }

    private final AtomicReference<VaultSecretsAccessor> secrets = new AtomicReference<>();
    private final AtomicReference<VaultAuthAccessor> auth = new AtomicReference<>();
    private final URL baseUrl;
    private final String apiVersion;
    private final VaultRequestExecutor executor;
    private final Duration requestTimeout;
    private final LogConfidentialityLevel logConfidentialityLevel;
    private final VaultTokenProvider tokenProvider;
    private final String namespace;
    private final InstantSource instantSource;

    private VaultClient(Builder builder) {
        this.baseUrl = Objects.requireNonNull(builder.baseUrl, "baseUrl is required");
        this.executor = Objects.requireNonNull(builder.executor, "executor is required");
        this.apiVersion = builder.apiVersion;
        this.logConfidentialityLevel = builder.logConfidentialityLevel;
        this.requestTimeout = builder.requestTimeout;
        this.tokenProvider = builder.tokenProvider;
        this.namespace = builder.namespace;
        this.instantSource = builder.instantSource;
    }

    public VaultSecretsAccessor secrets() {
        if (secrets.get() == null) {
            secrets.set(new VaultSecretsAccessor(this));
        }
        return secrets.get();
    }

    public VaultAuthAccessor auth() {
        if (auth.get() == null) {
            auth.set(new VaultAuthAccessor(this));
        }
        return auth.get();
    }

    public VaultSysAccessor sys() {
        return new VaultSysAccessor(this);
    }

    public URL getBaseUrl() {
        return baseUrl;
    }

    public String getApiVersion() {
        return apiVersion;
    }

    public VaultRequestExecutor getExecutor() {
        return executor;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public LogConfidentialityLevel getLogConfidentialityLevel() {
        return logConfidentialityLevel;
    }

    public VaultTokenProvider getTokenProvider() {
        return tokenProvider;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {

        var requestBuilder = request.builder();

        requestBuilder.baseUrl(baseUrl);
        if (requestTimeout != null) {
            requestBuilder.timeout(requestTimeout);
        }
        if (apiVersion != null) {
            requestBuilder.apiVersion(apiVersion);
        }
        if (logConfidentialityLevel != null) {
            requestBuilder.logConfidentialityLevel(logConfidentialityLevel);
        }

        if (!request.hasNamespace() && namespace != null) {
            requestBuilder.namespace(namespace);
        }

        if (request.hasToken() || tokenProvider == null) {
            return executor.execute(requestBuilder.rebuild());
        }

        var appliedToken = new AtomicReference<VaultToken>(null);

        var response = tokenProvider.apply(VaultAuthRequest.of(this, request, instantSource))
                .flatMap(token -> {

                    appliedToken.set(token);

                    if (token == null) {
                        requestBuilder.noToken();
                    } else {
                        requestBuilder.token(token.getClientToken());
                    }

                    return executor.execute(requestBuilder.rebuild());
                });

        return new UniRetryAtMost<>(response, e -> {
            if (e instanceof VaultClientException ve) {
                var token = appliedToken.get();
                return ve.isPermissionDenied() && token != null && token.isFromCache();
            }
            return false;
        }, 1).invoke(tokenProvider::invalidateCache);
    }

    public VaultClient.Builder configure() {
        var builder = new VaultClient.Builder();
        builder.baseUrl = baseUrl;
        builder.apiVersion = apiVersion;
        builder.executor = executor;
        builder.tokenProvider = tokenProvider;
        builder.namespace = namespace;
        builder.requestTimeout = requestTimeout;
        builder.logConfidentialityLevel = logConfidentialityLevel;
        return builder;
    }
}

package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAccessor.DEFAULT_GITHUB_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.auth.unwrap.VaultKeyValueUnwrappingValueProvider;
import io.quarkus.vault.client.auth.unwrap.VaultValueProvider;

public class VaultGithubAuthOptions extends VaultAuthOptions {

    public static class Builder {
        private String mountPath = DEFAULT_GITHUB_MOUNT_PATH;
        private Function<VaultAuthRequest, CompletionStage<String>> tokenProvider;
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public Builder token(String token) {
            this.tokenProvider = VaultValueProvider.staticValue(token);
            return this;
        }

        public Builder unwrappingToken(String wrappingToken, int kvVersion) {
            return unwrappingToken(wrappingToken, "token", kvVersion);
        }

        public Builder unwrappingToken(String wrappingToken, String kvKey, int kvVersion) {
            this.tokenProvider = new VaultKeyValueUnwrappingValueProvider(wrappingToken, kvKey, kvVersion);
            return this;
        }

        public Builder customTokenProvider(Function<VaultAuthRequest, CompletionStage<String>> tokenProvider) {
            this.tokenProvider = tokenProvider;
            return this;
        }

        public Builder caching(Duration cachingRenewGracePeriod) {
            this.cachingRenewGracePeriod = cachingRenewGracePeriod;
            return this;
        }

        public Builder noCaching() {
            this.cachingRenewGracePeriod = Duration.ZERO;
            return this;
        }

        public VaultGithubAuthOptions build() {
            return new VaultGithubAuthOptions(this);
        }
    }

    public final String mountPath;

    public final Function<VaultAuthRequest, CompletionStage<String>> tokenProvider;

    private VaultGithubAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.mountPath = builder.mountPath;
        this.tokenProvider = builder.tokenProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

}

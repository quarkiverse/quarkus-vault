package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.function.Function;

import io.quarkus.vault.client.auth.unwrap.VaultClientTokenUnwrappingProvider;
import io.quarkus.vault.client.auth.unwrap.VaultValueProvider;
import io.smallrye.mutiny.Uni;

public class VaultStaticClientTokenAuthOptions extends VaultAuthOptions {

    public static class Builder {
        private Function<VaultAuthRequest, Uni<String>> tokenProvider;
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder token(String token) {
            this.tokenProvider = VaultValueProvider.staticValue(token);
            return this;
        }

        public Builder unwrappingToken(String wrappedToken) {
            this.tokenProvider = new VaultClientTokenUnwrappingProvider(wrappedToken);
            return this;
        }

        public Builder customTokenProvider(Function<VaultAuthRequest, Uni<String>> tokenProvider) {
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

        public VaultStaticClientTokenAuthOptions build() {
            return new VaultStaticClientTokenAuthOptions(this);
        }
    }

    public final Function<VaultAuthRequest, Uni<String>> tokenProvider;

    private VaultStaticClientTokenAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.tokenProvider = builder.tokenProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

}

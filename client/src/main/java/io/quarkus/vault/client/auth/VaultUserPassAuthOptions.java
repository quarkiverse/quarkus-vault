package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAPIAccessor.DEFAULT_USERPASS_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.function.Function;

import io.quarkus.vault.client.auth.unwrap.VaultPasswordUnwrappingTokenProvider;
import io.quarkus.vault.client.auth.unwrap.VaultUnwrappedTokenProvider;
import io.smallrye.mutiny.Uni;

public class VaultUserPassAuthOptions extends VaultAuthOptions {

    public static class Builder {
        private String mountPath = DEFAULT_USERPASS_MOUNT_PATH;
        private String username;
        private Function<VaultAuthRequest, Uni<String>> passwordProvider;
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.passwordProvider = VaultUnwrappedTokenProvider.unwrapped(password);
            return this;
        }

        public Builder unwrappingPassword(String wrappingToken, int kvVersion) {
            this.passwordProvider = new VaultPasswordUnwrappingTokenProvider(wrappingToken, kvVersion);
            return this;
        }

        public Builder customPasswordProvider(Function<VaultAuthRequest, Uni<String>> passwordProvider) {
            this.passwordProvider = passwordProvider;
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

        public VaultUserPassAuthOptions build() {
            return new VaultUserPassAuthOptions(this);
        }
    }

    public final String mountPath;

    public final String username;
    public final Function<VaultAuthRequest, Uni<String>> passwordProvider;

    private VaultUserPassAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.mountPath = builder.mountPath;
        this.username = builder.username;
        this.passwordProvider = builder.passwordProvider;
    }

    public static Builder builder() {
        return new Builder();
    }

}

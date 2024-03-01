package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAccessor.DEFAULT_USERPASS_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.auth.unwrap.VaultKeyValueUnwrappingValueProvider;
import io.quarkus.vault.client.auth.unwrap.VaultValueProvider;

public class VaultUserPassAuthOptions extends VaultAuthOptions {

    public static class Builder {
        private String mountPath = DEFAULT_USERPASS_MOUNT_PATH;
        private String username;
        private Function<VaultAuthRequest, CompletionStage<String>> passwordProvider;
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
            this.passwordProvider = VaultValueProvider.staticValue(password);
            return this;
        }

        public Builder unwrappingPassword(String wrappingToken, int kvVersion) {
            return unwrappingPassword(wrappingToken, "password", kvVersion);
        }

        public Builder unwrappingPassword(String wrappingToken, String kvPath, int kvVersion) {
            this.passwordProvider = new VaultKeyValueUnwrappingValueProvider(wrappingToken, kvPath, kvVersion);
            return this;
        }

        public Builder customPasswordProvider(Function<VaultAuthRequest, CompletionStage<String>> passwordProvider) {
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
    public final Function<VaultAuthRequest, CompletionStage<String>> passwordProvider;

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

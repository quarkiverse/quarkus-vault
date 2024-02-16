package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAccessor.DEFAULT_APPROLE_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.auth.unwrap.VaultSecretIdUnwrappingProvider;
import io.quarkus.vault.client.auth.unwrap.VaultValueProvider;

public class VaultAppRoleAuthOptions extends VaultAuthOptions {

    public static class Builder {
        private String mountPath = DEFAULT_APPROLE_MOUNT_PATH;
        private String roleId;
        private Function<VaultAuthRequest, CompletionStage<String>> secretIdProvider;
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public Builder roleId(String roleId) {
            this.roleId = roleId;
            return this;
        }

        public Builder secretId(String secretId) {
            this.secretIdProvider = VaultValueProvider.staticValue(secretId);
            return this;
        }

        public Builder unwrappingSecretId(String wrappingToken) {
            this.secretIdProvider = new VaultSecretIdUnwrappingProvider(wrappingToken);
            return this;
        }

        public Builder customSecretIdProvider(Function<VaultAuthRequest, CompletionStage<String>> secretIdProvider) {
            this.secretIdProvider = secretIdProvider;
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

        public VaultAppRoleAuthOptions build() {
            return new VaultAppRoleAuthOptions(this);
        }
    }

    public final String mountPath;
    public final String roleId;
    public final Function<VaultAuthRequest, CompletionStage<String>> secretIdProvider;

    private VaultAppRoleAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.mountPath = Objects.requireNonNull(builder.mountPath);
        this.roleId = Objects.requireNonNull(builder.roleId);
        this.secretIdProvider = Objects.requireNonNull(builder.secretIdProvider);
    }

    public static Builder builder() {
        return new Builder();
    }
}

package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.api.VaultAuthAccessor.DEFAULT_KUBERNETES_MOUNT_PATH;
import static io.quarkus.vault.client.auth.VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD;
import static io.quarkus.vault.client.auth.VaultKubernetesTokenProvider.jwtTokenPathReader;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

public class VaultKubernetesAuthOptions extends VaultAuthOptions {

    public static final Path DEFAULT_KUBERNETES_JWT_TOKEN_PATH = Path.of("/var/run/secrets/kubernetes.io/serviceaccount/token");

    public static class Builder {
        private String mountPath = DEFAULT_KUBERNETES_MOUNT_PATH;
        private String role;
        private Supplier<Uni<String>> jwtProvider = jwtTokenPathReader(DEFAULT_KUBERNETES_JWT_TOKEN_PATH);
        private Duration cachingRenewGracePeriod = DEFAULT_RENEW_GRACE_PERIOD;

        public Builder mountPath(String mountPath) {
            this.mountPath = mountPath;
            return this;
        }

        public Builder role(String role) {
            this.role = role;
            return this;
        }

        public Builder jwtTokenPath(Path jwtTokenPath) {
            this.jwtProvider = jwtTokenPathReader(jwtTokenPath);
            return this;
        }

        public Builder jwtProvider(Supplier<Uni<String>> jwtProvider) {
            this.jwtProvider = jwtProvider;
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

        public VaultKubernetesAuthOptions build() {
            return new VaultKubernetesAuthOptions(this);
        }
    }

    public final String mountPath;
    public final String role;
    public final Supplier<Uni<String>> jwtProvider;

    private VaultKubernetesAuthOptions(Builder builder) {
        super(builder.cachingRenewGracePeriod);
        this.mountPath = Objects.requireNonNull(builder.mountPath);
        this.role = Objects.requireNonNull(builder.role);
        this.jwtProvider = Objects.requireNonNull(builder.jwtProvider);
    }

    public static Builder builder() {
        return new Builder();
    }

}

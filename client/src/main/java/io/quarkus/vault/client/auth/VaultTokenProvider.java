package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public interface VaultTokenProvider extends Function<VaultAuthRequest, Uni<VaultToken>> {

    default VaultTokenProvider caching(Duration renewGracePeriod) {
        return new VaultCachingTokenProvider(this, renewGracePeriod);
    }

    default void invalidateCache() {
    }

}

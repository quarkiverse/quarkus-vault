package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public interface VaultTokenProvider extends Function<VaultAuthRequest, CompletionStage<VaultToken>> {

    default VaultTokenProvider caching(Duration renewGracePeriod) {
        return new VaultCachingTokenProvider(this, renewGracePeriod);
    }

    default void invalidateCache() {
    }

}

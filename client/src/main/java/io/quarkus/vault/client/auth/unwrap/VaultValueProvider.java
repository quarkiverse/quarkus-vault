package io.quarkus.vault.client.auth.unwrap;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.auth.VaultAuthRequest;

public interface VaultValueProvider extends Function<VaultAuthRequest, CompletionStage<String>> {

    static VaultValueProvider staticValue(String unwrappedToken) {
        return new StaticValueProvider(unwrappedToken);
    }

    class StaticValueProvider implements VaultValueProvider {
        private final String unwrappedToken;

        public StaticValueProvider(String unwrappedToken) {
            this.unwrappedToken = unwrappedToken;
        }

        @Override
        public CompletionStage<String> apply(VaultAuthRequest request) {
            return CompletableFuture.completedStage(unwrappedToken);
        }
    }

}

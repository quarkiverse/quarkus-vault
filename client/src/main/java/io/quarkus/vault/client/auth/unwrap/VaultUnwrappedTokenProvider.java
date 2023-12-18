package io.quarkus.vault.client.auth.unwrap;

import java.util.function.Function;

import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.smallrye.mutiny.Uni;

public interface VaultUnwrappedTokenProvider extends Function<VaultAuthRequest, Uni<String>> {

    static VaultUnwrappedTokenProvider unwrapped(String unwrappedToken) {
        return new UnwrappedTokenProvider(unwrappedToken);
    }

    class UnwrappedTokenProvider implements VaultUnwrappedTokenProvider {
        private final String unwrappedToken;

        public UnwrappedTokenProvider(String unwrappedToken) {
            this.unwrappedToken = unwrappedToken;
        }

        @Override
        public Uni<String> apply(VaultAuthRequest request) {
            return Uni.createFrom().item(unwrappedToken);
        }
    }

}

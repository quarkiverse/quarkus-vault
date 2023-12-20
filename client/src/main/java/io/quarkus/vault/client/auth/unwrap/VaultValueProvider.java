package io.quarkus.vault.client.auth.unwrap;

import java.util.function.Function;

import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.smallrye.mutiny.Uni;

public interface VaultValueProvider extends Function<VaultAuthRequest, Uni<String>> {

    static VaultValueProvider staticValue(String unwrappedToken) {
        return new StaticValueProvider(unwrappedToken);
    }

    class StaticValueProvider implements VaultValueProvider {
        private final String unwrappedToken;

        public StaticValueProvider(String unwrappedToken) {
            this.unwrappedToken = unwrappedToken;
        }

        @Override
        public Uni<String> apply(VaultAuthRequest request) {
            return Uni.createFrom().item(unwrappedToken);
        }
    }

}

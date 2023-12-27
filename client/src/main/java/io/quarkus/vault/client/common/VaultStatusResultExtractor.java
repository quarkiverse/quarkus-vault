package io.quarkus.vault.client.common;

import java.util.Optional;

public class VaultStatusResultExtractor implements VaultResultExtractor<Integer> {

    public static final VaultStatusResultExtractor INSTANCE = new VaultStatusResultExtractor();

    @Override
    public Optional<Integer> extract(VaultResponse<Integer> response) {
        return Optional.of(response.getStatusCode());
    }
}

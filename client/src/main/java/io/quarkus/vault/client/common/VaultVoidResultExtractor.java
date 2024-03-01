package io.quarkus.vault.client.common;

import java.util.Optional;

public class VaultVoidResultExtractor implements VaultResultExtractor<Void> {

    public static final VaultVoidResultExtractor INSTANCE = new VaultVoidResultExtractor();

    @Override
    public Optional<Void> extract(VaultResponse<Void> response) {
        return Optional.empty();
    }
}

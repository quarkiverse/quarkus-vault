package io.quarkus.vault.client.common;

import java.util.Optional;

public class VaultBinaryResultExtractor implements VaultResultExtractor<byte[]> {

    @Override
    public Optional<byte[]> extract(VaultResponse<byte[]> response) {
        return response.getBody();
    }
}

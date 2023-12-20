package io.quarkus.vault.client.common;

public class VaultStatusResultExtractor implements VaultResultExtractor<Integer> {

    public static final VaultStatusResultExtractor INSTANCE = new VaultStatusResultExtractor();

    @Override
    public Integer extract(VaultResponse<Integer> response) {
        return response.statusCode;
    }
}

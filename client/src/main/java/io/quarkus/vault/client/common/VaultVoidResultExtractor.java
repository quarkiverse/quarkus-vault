package io.quarkus.vault.client.common;

class VaultVoidResultExtractor implements VaultResultExtractor<Void> {

    public static final VaultVoidResultExtractor INSTANCE = new VaultVoidResultExtractor();

    @Override
    public Void extract(VaultResponse<Void> response) {
        return null;
    }
}

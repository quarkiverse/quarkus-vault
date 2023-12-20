package io.quarkus.vault.client.common;

class VaultBinaryResultExtractor implements VaultResultExtractor<byte[]> {

    @Override
    public byte[] extract(VaultResponse<byte[]> response) {
        return response.body;
    }
}

package io.quarkus.vault.client.common;

public class VaultBinaryResult implements VaultResult {

    private final byte[] data;

    public VaultBinaryResult(byte[] data) {
        this.data = data;
    }

    public byte[] getData() {
        return data;
    }

}

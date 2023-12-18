package io.quarkus.vault.client.common;

public class VaultStatusResult implements VaultResult {

    private final int statusCode;

    public VaultStatusResult(int statusCode) {
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

}

package io.quarkus.vault.transit;

public class VaultTransitDataKey {

    private String ciphertext;

    private String plaintext;

    public String getCiphertext() {
        return ciphertext;
    }

    public VaultTransitDataKey setCiphertext(String ciphertext) {
        this.ciphertext = ciphertext;
        return this;
    }

    public String getPlaintext() {
        return plaintext;
    }

    public VaultTransitDataKey setPlaintext(String plaintext) {
        this.plaintext = plaintext;
        return this;
    }
}

package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecretsDatabaseRSAPrivateKeyCredentialConfig implements VaultSecretsDatabaseCredentialConfig {

    public enum Format {
        @JsonProperty("pkcs8")
        PKCS8
    }

    @Override
    public VaultSecretsDatabaseCredentialType getType() {
        return VaultSecretsDatabaseCredentialType.RSA_PRIVATE_KEY;
    }

    @JsonProperty("key_bits")
    private KeyBits keyBits;

    @JsonProperty("format")
    private Format format;

    public KeyBits getKeyBits() {
        return keyBits;
    }

    public VaultSecretsDatabaseRSAPrivateKeyCredentialConfig setKeyBits(KeyBits keyBits) {
        this.keyBits = keyBits;
        return this;
    }

    public Format getFormat() {
        return format;
    }

    public VaultSecretsDatabaseRSAPrivateKeyCredentialConfig setFormat(Format format) {
        this.format = format;
        return this;
    }
}

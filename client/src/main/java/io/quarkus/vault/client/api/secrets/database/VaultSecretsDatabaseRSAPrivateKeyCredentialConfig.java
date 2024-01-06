package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

public class VaultSecretsDatabaseRSAPrivateKeyCredentialConfig implements VaultSecretsDatabaseCredentialConfig {

    public enum Format implements VaultModel {

        PKCS8("pkcs8");

        private final String value;

        Format(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue();
        }

        @JsonCreator
        public static Format from(String value) {
            if (value == null)
                return null;
            for (var v : values()) {
                if (v.value.equals(value))
                    return v;
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
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

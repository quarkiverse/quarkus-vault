package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

@JsonSubTypes({
        @JsonSubTypes.Type(value = VaultSecretsDatabasePasswordCredentialConfig.class, name = "password"),
        @JsonSubTypes.Type(value = VaultSecretsDatabaseRSAPrivateKeyCredentialConfig.class, name = "rsa_private_key"),
        @JsonSubTypes.Type(value = VaultSecretsDatabaseClientCertificateCredentialConfig.class, name = "client_certificate"),
})
public interface VaultSecretsDatabaseCredentialConfig extends VaultModel {

    enum KeyBits implements VaultModel {
        BITS_2048("2048"),
        BITS_3072("3072"),
        BITS_4096("4096");

        private final String value;

        KeyBits(String value) {
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
        public static KeyBits from(String value) {
            if (value == null)
                return null;
            for (var v : values()) {
                if (v.value.equals(value))
                    return v;
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    VaultSecretsDatabaseCredentialType getType();

}

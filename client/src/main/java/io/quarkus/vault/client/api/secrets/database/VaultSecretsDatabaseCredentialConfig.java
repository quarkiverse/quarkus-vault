package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({
        @JsonSubTypes.Type(value = VaultSecretsDatabasePasswordCredentialConfig.class, name = "password"),
        @JsonSubTypes.Type(value = VaultSecretsDatabaseRSAPrivateKeyCredentialConfig.class, name = "rsa_private_key"),
        @JsonSubTypes.Type(value = VaultSecretsDatabaseClientCertificateCredentialConfig.class, name = "client_certificate"),
})
public interface VaultSecretsDatabaseCredentialConfig {

    enum KeyBits {
        @JsonProperty("2048")
        BITS_2048,
        @JsonProperty("3072")
        BITS_3072,
        @JsonProperty("4096")
        BITS_4096
    }

    VaultSecretsDatabaseCredentialType getType();

}

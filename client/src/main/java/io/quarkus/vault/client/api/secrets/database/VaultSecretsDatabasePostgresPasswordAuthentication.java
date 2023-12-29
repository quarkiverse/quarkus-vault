package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VaultSecretsDatabasePostgresPasswordAuthentication {
    @JsonProperty("scram-sha-256")
    SCRAM_SHA_256,
    @JsonProperty("password")
    PASSWORD
}

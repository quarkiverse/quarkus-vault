package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum VaultSecretsDatabaseAuthType {
    @JsonProperty("gcp_iam")
    GCP_IAM
}

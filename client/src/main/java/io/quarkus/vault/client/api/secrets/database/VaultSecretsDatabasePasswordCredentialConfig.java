package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecretsDatabasePasswordCredentialConfig implements VaultSecretsDatabaseCredentialConfig {

    @JsonProperty("password_policy")
    private String passwordPolicy;

    @Override
    public VaultSecretsDatabaseCredentialType getType() {
        return VaultSecretsDatabaseCredentialType.PASSWORD;
    }

    public String getPasswordPolicy() {
        return passwordPolicy;
    }

    public VaultSecretsDatabasePasswordCredentialConfig setPasswordPolicy(String passwordPolicy) {
        this.passwordPolicy = passwordPolicy;
        return this;
    }

}

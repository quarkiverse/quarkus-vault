package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthReadSecretIdData implements VaultModel {

    @JsonProperty("secret_id")
    public String secretId;

    public String getSecretId() {
        return secretId;
    }

    public VaultAppRoleAuthReadSecretIdData setSecretId(String secretId) {
        this.secretId = secretId;
        return this;
    }
}

package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthReadSecretIdAccessorData implements VaultModel {

    @JsonProperty("secret_id_accessor")
    public String secretIdAccessor;

    public String getSecretIdAccessor() {
        return secretIdAccessor;
    }

    public VaultAppRoleAuthReadSecretIdAccessorData setSecretIdAccessor(String secretIdAccessor) {
        this.secretIdAccessor = secretIdAccessor;
        return this;
    }
}

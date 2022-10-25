package io.quarkus.vault.runtime.client.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthRoleIdData implements VaultModel {

    @JsonProperty("role_id")
    public String roleId;

    public String getRoleId() {
        return roleId;
    }

    public VaultAppRoleAuthRoleIdData setRoleId(String roleId) {
        this.roleId = roleId;
        return this;
    }
}

package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthListRolesData implements VaultModel {

    public List<String> keys;
}

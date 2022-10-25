package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthListSecretIdAccessorsData implements VaultModel {

    public List<String> keys;
}

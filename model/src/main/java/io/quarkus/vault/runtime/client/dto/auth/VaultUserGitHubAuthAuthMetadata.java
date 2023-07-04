package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUserGitHubAuthAuthMetadata implements VaultModel {

    public String username;

    public String org;

}

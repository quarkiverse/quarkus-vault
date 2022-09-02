package io.quarkus.vault.runtime.client.dto.auth;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultUserGitHubAuthBody implements VaultModel {

    public VaultUserGitHubAuthBody(String token) {
        this.token = token;
    }

    public String token;

}

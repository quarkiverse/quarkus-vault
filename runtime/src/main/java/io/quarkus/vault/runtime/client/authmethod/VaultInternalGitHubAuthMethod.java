package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserGitHubAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserGitHubAuthBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalGitHubAuthMethod extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (user/pass)]";
    }

    public Uni<VaultUserGitHubAuth> login(VaultClient vaultClient, String token) {
        VaultUserGitHubAuthBody body = new VaultUserGitHubAuthBody(token);
        return vaultClient.post(opName("Login"), "auth/github/login", null, body, VaultUserGitHubAuth.class);
    }
}

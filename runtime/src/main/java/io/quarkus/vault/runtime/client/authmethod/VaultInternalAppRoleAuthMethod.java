package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalAppRoleAuthMethod extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (approle)]";
    }

    public Uni<VaultAppRoleAuth> login(VaultClient vaultClient, String roleId, String secretId) {
        VaultAppRoleAuthBody body = new VaultAppRoleAuthBody(roleId, secretId);
        return vaultClient.post(opName("Login"), "auth/approle/login", null, body, VaultAppRoleAuth.class);
    }
}

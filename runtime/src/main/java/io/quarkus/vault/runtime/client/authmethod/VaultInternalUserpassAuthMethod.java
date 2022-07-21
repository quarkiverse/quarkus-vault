package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuthBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalUserpassAuthMethod extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (user/pass)]";
    }

    public Uni<VaultUserPassAuth> login(VaultClient vaultClient, String user, String password) {
        VaultUserPassAuthBody body = new VaultUserPassAuthBody(password);
        return vaultClient.post(opName("Login"), "auth/userpass/login/" + user, null, body, VaultUserPassAuth.class);
    }
}

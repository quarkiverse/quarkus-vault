package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuthBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalUserpassAuthMethod extends VaultInternalBase {

    public Uni<VaultUserPassAuth> login(String user, String password) {
        VaultUserPassAuthBody body = new VaultUserPassAuthBody(password);
        return vaultClient.post("auth/userpass/login/" + user, null, body, VaultUserPassAuth.class);
    }
}

package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalTokenAuthMethod extends VaultInternalBase {

    public Uni<VaultRenewSelf> renewSelf(String token, String increment) {
        VaultRenewSelfBody body = new VaultRenewSelfBody(increment);
        return vaultClient.post("auth/token/renew-self", token, body, VaultRenewSelf.class);
    }

    public Uni<VaultLookupSelf> lookupSelf(String token) {
        return vaultClient.get("auth/token/lookup-self", token, VaultLookupSelf.class);
    }
}

package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelfBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalTokenAuthMethod extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (token)]";
    }

    public Uni<VaultRenewSelf> renewSelf(VaultClient vaultClient, String token, String increment) {
        VaultRenewSelfBody body = new VaultRenewSelfBody(increment);
        return vaultClient.post(opName("Renew Self"), "auth/token/renew-self", token, body, VaultRenewSelf.class);
    }

    public Uni<VaultLookupSelf> lookupSelf(VaultClient vaultClient, String token) {
        return vaultClient.get(opName("Lookup Self"), "auth/token/lookup-self", token, VaultLookupSelf.class);
    }
}

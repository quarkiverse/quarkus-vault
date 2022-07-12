package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.dynamic.VaultDynamicCredentials;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalDynamicCredentialsSecretEngine extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [CREDENTIALS]";
    }

    public Uni<VaultDynamicCredentials> generateCredentials(VaultClient vaultClient, String token, String mount,
            String requestPath, String role) {
        return vaultClient.get(opName("Generate"), mount + "/" + requestPath + "/" + role, token,
                VaultDynamicCredentials.class);
    }
}

package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvListSecrets;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2Write;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalKvV2SecretEngine extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [KV (v2)]";
    }

    public Uni<VaultKvSecretV2> getSecret(VaultClient vaultClient, String token, String secretEnginePath, String path) {
        return vaultClient.get(opName("Get Secret"), secretEnginePath + "/data/" + path, token, VaultKvSecretV2.class);
    }

    public Uni<Void> writeSecret(VaultClient vaultClient, String token, String secretEnginePath, String path,
            VaultKvSecretV2WriteBody body) {
        return vaultClient
                .post(opName("Write Secret"), secretEnginePath + "/data/" + path, token, body, VaultKvSecretV2Write.class)
                .replaceWithVoid();
    }

    public Uni<Void> deleteSecret(VaultClient vaultClient, String token, String secretEnginePath, String path) {
        return vaultClient.delete(opName("Delete Secret"), secretEnginePath + "/data/" + path, token, 204);
    }

    public Uni<VaultKvListSecrets> listSecrets(VaultClient vaultClient, String token, String secretEnginePath, String path) {
        return vaultClient.list(opName("List Secrets"), secretEnginePath + "/metadata/" + path, token,
                VaultKvListSecrets.class);
    }
}

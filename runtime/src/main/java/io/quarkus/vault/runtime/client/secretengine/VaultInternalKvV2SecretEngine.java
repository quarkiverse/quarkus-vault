package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvListSecrets;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2Write;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalKvV2SecretEngine extends VaultInternalBase {

    public Uni<VaultKvSecretV2> getSecret(String token, String secretEnginePath, String path) {
        return vaultClient.get(secretEnginePath + "/data/" + path, token, VaultKvSecretV2.class);
    }

    public Uni<Void> writeSecret(String token, String secretEnginePath, String path, VaultKvSecretV2WriteBody body) {
        return vaultClient.post(secretEnginePath + "/data/" + path, token, body, VaultKvSecretV2Write.class)
                .replaceWithVoid();
    }

    public Uni<Void> deleteSecret(String token, String secretEnginePath, String path) {
        return vaultClient.delete(secretEnginePath + "/data/" + path, token, 204);
    }

    public Uni<VaultKvListSecrets> listSecrets(String token, String secretEnginePath, String path) {
        return vaultClient.list(secretEnginePath + "/metadata/" + path, token, VaultKvListSecrets.class);
    }
}

package io.quarkus.vault.runtime.client.secretengine;

import java.util.Map;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvListSecrets;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV1;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalKvV1SecretEngine extends VaultInternalBase {

    public Uni<Void> deleteSecret(String token, String secretEnginePath, String path) {
        return vaultClient.delete(secretEnginePath + "/" + path, token, 204);
    }

    public Uni<Void> writeSecret(String token, String secretEnginePath, String path, Map<String, String> secret) {
        return vaultClient.post(secretEnginePath + "/" + path, token, secret, null, 204);
    }

    public Uni<VaultKvSecretV1> getSecret(String token, String secretEnginePath, String path) {
        return vaultClient.get(secretEnginePath + "/" + path, token, VaultKvSecretV1.class);
    }

    public Uni<VaultKvListSecrets> listSecrets(String token, String secretEnginePath, String path) {
        return vaultClient.list(secretEnginePath + "/" + path, token, VaultKvListSecrets.class);
    }
}

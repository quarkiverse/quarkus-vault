package io.quarkus.vault.runtime.kv;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretData;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2RequestFactory;
import io.smallrye.mutiny.Uni;

public class KvV2 extends VersionedKv<VaultSecretsKV2RequestFactory> {

    private final VaultSecretsKV2 kvv2;

    public KvV2(VaultSecretsKV2 kvv2) {
        this.kvv2 = kvv2;
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String path) {
        return Uni.createFrom().completionStage(kvv2.readSecret(path)).map(VaultSecretsKV2ReadSecretData::getData);
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return Uni.createFrom().completionStage(kvv2.updateSecret(path, null, asSecretMap(secret))).map(r -> null);
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return Uni.createFrom().completionStage(kvv2.deleteSecret(path));
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return Uni.createFrom().completionStage(kvv2.listSecrets(path));
    }
}

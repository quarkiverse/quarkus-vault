package io.quarkus.vault.runtime.kv;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1;
import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1RequestFactory;
import io.smallrye.mutiny.Uni;

public class KvV1 extends VersionedKv<VaultSecretsKV1RequestFactory> {

    private final VaultSecretsKV1 kvv1;

    public KvV1(VaultSecretsKV1 kvv1) {
        this.kvv1 = kvv1;
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String path) {
        return Uni.createFrom().completionStage(kvv1.read(path));
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return Uni.createFrom().completionStage(kvv1.update(path, asSecretMap(secret)));
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return Uni.createFrom().completionStage(kvv1.delete(path));
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return Uni.createFrom().completionStage(kvv1.list(path));
    }
}

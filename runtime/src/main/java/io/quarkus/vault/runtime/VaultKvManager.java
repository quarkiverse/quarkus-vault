package io.quarkus.vault.runtime;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretData;
import io.quarkus.vault.runtime.client.Private;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretReactiveEngine {

    @Produces
    @Private
    public static VaultKvManager privateClientManager(@Private VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        return new VaultKvManager(vaultClient, vaultConfigHolder);
    }

    private final VaultSecretsKV1 kv1;
    private final VaultSecretsKV2 kv2;
    private final boolean isV1;

    public VaultKvManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        this.kv1 = vaultClient.secrets().kv1(vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineMountPath());
        this.kv2 = vaultClient.secrets().kv2(vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineMountPath());
        this.isV1 = vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineVersion() == 1;
    }

    private Map<String, String> convert(Map<String, Object> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue()));
    }

    @Override
    public Uni<Map<String, String>> readSecret(String path) {
        return readSecretJson(path).map(this::convert);
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String path) {
        return isV1 ? kv1.read(path) : kv2.readSecret(path).map(VaultSecretsKV2ReadSecretData::getData);
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        var secretMap = secret.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue()));
        return isV1 ? kv1.update(path, secretMap) : kv2.updateSecret(path, null, secretMap).map(r -> null);
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return isV1 ? kv1.delete(path) : kv2.deleteSecret(path);
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return isV1 ? kv1.list(path) : kv2.listSecrets(path);
    }
}

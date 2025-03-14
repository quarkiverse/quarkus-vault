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
    private final Map<String, VaultSecretsKV1> kv1Prefix;
    private final Map<String, VaultSecretsKV2> kv2Prefix;
    private final boolean isV1;

    public VaultKvManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        this.kv1 = vaultClient.secrets().kv1(vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineMountPath());
        this.kv2 = vaultClient.secrets().kv2(vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineMountPath());
        this.kv1Prefix = vaultConfigHolder
                .getVaultRuntimeConfig()
                .kvSecretEngineMountPathPrefix()
                .entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> vaultClient.secrets().kv1(e.getValue())));
        this.kv2Prefix = vaultConfigHolder
                .getVaultRuntimeConfig()
                .kvSecretEngineMountPathPrefix()
                .entrySet()
                .stream()
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, e -> vaultClient.secrets().kv2(e.getValue())));
        this.isV1 = vaultConfigHolder.getVaultRuntimeConfig().kvSecretEngineVersion() == 1;
    }

    private Map<String, String> convert(Map<String, Object> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue()));
    }

    private VaultSecretsKV1 kv1(String prefix) {
        var kv1 = prefix == null ? this.kv1 : kv1Prefix.get(prefix);
        if (kv1 == null) {
            throw new IllegalStateException("Invalid prefix \"" + prefix
                    + "\" or missing config property quarkus.vault.kv-secret-engine-mount-path.\"" + prefix + "\"");
        }
        return kv1;
    }

    private VaultSecretsKV2 kv2(String prefix) {
        var kv2 = prefix == null ? this.kv2 : kv2Prefix.get(prefix);
        if (kv2 == null) {
            throw new IllegalStateException("Invalid prefix \"" + prefix
                    + "\" or missing config property quarkus.vault.kv-secret-engine-mount-path.\"" + prefix + "\"");
        }
        return kv2;
    }

    @Override
    public Uni<Map<String, String>> readSecret(String path) {
        return readSecret(null, path);
    }

    @Override
    public Uni<Map<String, String>> readSecret(String prefix, String path) {
        return readSecretJson(prefix, path).map(this::convert);
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String path) {
        return readSecretJson(null, path);
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String prefix, String path) {
        return isV1 ? Uni.createFrom().completionStage(kv1(prefix).read(path))
                : Uni.createFrom().completionStage(kv2(prefix).readSecret(path)).map(VaultSecretsKV2ReadSecretData::getData);
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return writeSecret(null, path, secret);
    }

    @Override
    public Uni<Void> writeSecret(String prefix, String path, Map<String, String> secret) {
        var secretMap = secret.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue()));
        return isV1 ? Uni.createFrom().completionStage(kv1(prefix).update(path, secretMap))
                : Uni.createFrom().completionStage(kv2(prefix).updateSecret(path, null, secretMap)).map(r -> null);
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return deleteSecret(null, path);
    }

    @Override
    public Uni<Void> deleteSecret(String prefix, String path) {
        return isV1 ? Uni.createFrom().completionStage(kv1(prefix).delete(path))
                : Uni.createFrom().completionStage(kv2(prefix).deleteSecret(path));
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return listSecrets(null, path);
    }

    @Override
    public Uni<List<String>> listSecrets(String prefix, String path) {
        return isV1 ? Uni.createFrom().completionStage(kv1(prefix).list(path))
                : Uni.createFrom().completionStage(kv2(prefix).listSecrets(path));
    }
}

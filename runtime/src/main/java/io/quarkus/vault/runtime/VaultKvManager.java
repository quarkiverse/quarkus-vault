package io.quarkus.vault.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.common.VaultRequestFactory;
import io.quarkus.vault.runtime.client.Private;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.kv.KvV1;
import io.quarkus.vault.runtime.kv.KvV2;
import io.quarkus.vault.runtime.kv.VersionedKv;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretReactiveEngine {

    public static final String DEFAULT = "<default>";

    @Produces
    @Private
    public static VaultKvManager privateClientManager(@Private VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        return new VaultKvManager(vaultClient, vaultConfigHolder);
    }

    private final Map<String, VersionedKv<? extends VaultRequestFactory>> engines = new HashMap<>();

    public VaultKvManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {

        VaultRuntimeConfig config = vaultConfigHolder.getVaultRuntimeConfig();

        // default engine
        putEngine(DEFAULT, vaultClient, config.kvSecretEngineVersion(), config.kvSecretEngineMountPath());

        // named engines
        for (var entry : config.kvSecretEngineAlias().entrySet()) {
            String alias = entry.getKey();
            var engineConfig = entry.getValue();
            putEngine(alias, vaultClient, engineConfig.version(), engineConfig.mountPath());
        }
    }

    private void putEngine(String alias, VaultClient vaultClient, int version, String mountPath) {
        if (version == 1) {
            engines.put(alias, new KvV1(vaultClient.secrets().kv1(mountPath)));
        } else {
            engines.put(alias, new KvV2(vaultClient.secrets().kv2(mountPath)));
        }
    }

    VersionedKv<? extends VaultRequestFactory> getEngine(String alias) {
        return Objects.requireNonNull(engines.get(alias));
    }

    @Override
    public Uni<Map<String, String>> readSecret(String path) {
        return readSecret(DEFAULT, path);
    }

    @Override
    public Uni<Map<String, String>> readSecret(String alias, String path) {
        return getEngine(alias).readSecret(path);
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String path) {
        return readSecretJson(DEFAULT, path);
    }

    @Override
    public Uni<Map<String, Object>> readSecretJson(String alias, String path) {
        return getEngine(alias).readSecretJson(path);
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return writeSecret(DEFAULT, path, secret);
    }

    @Override
    public Uni<Void> writeSecret(String alias, String path, Map<String, String> secret) {
        return getEngine(alias).writeSecret(path, secret);
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return deleteSecret(DEFAULT, path);
    }

    @Override
    public Uni<Void> deleteSecret(String alias, String path) {
        return getEngine(alias).deleteSecret(path);
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return listSecrets(DEFAULT, path);
    }

    @Override
    public Uni<List<String>> listSecrets(String alias, String path) {
        return getEngine(alias).listSecrets(path);
    }
}

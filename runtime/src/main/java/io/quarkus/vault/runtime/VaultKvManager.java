package io.quarkus.vault.runtime;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.runtime.client.Private;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretReactiveEngine {

    @Produces
    @Private
    public static VaultKvManager privateClientManager(@Private VaultClient vaultClient,
            VaultAuthManager vaultAuthManager,
            VaultConfigHolder vaultConfigHolder,
            VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine,
            VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine) {
        return new VaultKvManager(vaultClient,
                vaultAuthManager,
                vaultConfigHolder,
                vaultInternalKvV1SecretEngine,
                vaultInternalKvV2SecretEngine);
    }

    private VaultClient vaultClient;
    private VaultAuthManager vaultAuthManager;
    private VaultConfigHolder vaultConfigHolder;
    private VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    private VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;

    public VaultKvManager(VaultClient vaultClient, VaultAuthManager vaultAuthManager,
            VaultConfigHolder vaultConfigHolder,
            VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine,
            VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine) {
        this.vaultClient = vaultClient;
        this.vaultAuthManager = vaultAuthManager;
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultInternalKvV1SecretEngine = vaultInternalKvV1SecretEngine;
        this.vaultInternalKvV2SecretEngine = vaultInternalKvV2SecretEngine;
    }

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    @Override
    public Uni<Map<String, String>> readSecret(String path) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.getSecret(vaultClient, token, mount, path).map(r -> r.data);
            } else {
                return vaultInternalKvV2SecretEngine.getSecret(vaultClient, token, mount, path).map(r -> r.data.data);
            }
        });
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.writeSecret(vaultClient, token, mount, path, secret);
            } else {
                VaultKvSecretV2WriteBody body = new VaultKvSecretV2WriteBody();
                body.data = secret;
                return vaultInternalKvV2SecretEngine.writeSecret(vaultClient, token, mount, path, body);
            }
        });
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.deleteSecret(vaultClient, token, mount, path);
            } else {
                return vaultInternalKvV2SecretEngine.deleteSecret(vaultClient, token, mount, path);
            }
        });
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            return (isV1()
                    ? vaultInternalKvV1SecretEngine.listSecrets(vaultClient, token, mount, path)
                    : vaultInternalKvV2SecretEngine.listSecrets(vaultClient, token, mount, path)).map(r -> r.data.keys);
        });
    }

    private boolean isV1() {
        return getConfig().kvSecretEngineVersion == 1;
    }
}

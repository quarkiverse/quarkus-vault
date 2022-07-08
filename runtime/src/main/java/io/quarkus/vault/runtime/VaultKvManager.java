package io.quarkus.vault.runtime;

import java.util.List;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretV2WriteBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKvManager implements VaultKVSecretReactiveEngine {

    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultConfigHolder vaultConfigHolder;
    @Inject
    private VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    @Inject
    private VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    @Override
    public Uni<Map<String, String>> readSecret(String path) {
        return vaultAuthManager.getClientToken().flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.getSecret(token, mount, path).map(r -> r.data);
            } else {
                return vaultInternalKvV2SecretEngine.getSecret(token, mount, path).map(r -> r.data.data);
            }
        });
    }

    @Override
    public Uni<Void> writeSecret(String path, Map<String, String> secret) {
        return vaultAuthManager.getClientToken().flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.writeSecret(token, mount, path, secret);
            } else {
                VaultKvSecretV2WriteBody body = new VaultKvSecretV2WriteBody();
                body.data = secret;
                return vaultInternalKvV2SecretEngine.writeSecret(token, mount, path, body);
            }
        });
    }

    @Override
    public Uni<Void> deleteSecret(String path) {
        return vaultAuthManager.getClientToken().flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            if (isV1()) {
                return vaultInternalKvV1SecretEngine.deleteSecret(token, mount, path);
            } else {
                return vaultInternalKvV2SecretEngine.deleteSecret(token, mount, path);
            }
        });
    }

    @Override
    public Uni<List<String>> listSecrets(String path) {
        return vaultAuthManager.getClientToken().flatMap(token -> {

            String mount = getConfig().kvSecretEngineMountPath;

            return (isV1()
                    ? vaultInternalKvV1SecretEngine.listSecrets(token, mount, path)
                    : vaultInternalKvV2SecretEngine.listSecrets(token, mount, path)).map(r -> r.data.keys);
        });
    }

    private boolean isV1() {
        return getConfig().kvSecretEngineVersion == 1;
    }
}

package io.quarkus.vault.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.VaultKVSecretReactiveEngine;
import io.quarkus.vault.VaultKvSecretEngineFactory;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;

@ApplicationScoped
public class VaultKvManagerFactory implements VaultKvSecretEngineFactory {

    @Inject
    VaultClient vaultClient;
    @Inject
    VaultAuthManager vaultAuthManager;
    @Inject
    VaultConfigHolder vaultConfigHolder;
    @Inject
    VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    @Inject
    VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;

    @Override
    public VaultKVSecretEngine engine(String mount) {
        return new VaultKVSecretEngine(reactiveEngine(mount));
    }

    @Override
    public VaultKVSecretReactiveEngine reactiveEngine(String mount) {
        return VaultKvManager.of(
                mount,
                vaultClient,
                vaultAuthManager,
                vaultConfigHolder,
                vaultInternalKvV1SecretEngine,
                vaultInternalKvV2SecretEngine);
    }
}

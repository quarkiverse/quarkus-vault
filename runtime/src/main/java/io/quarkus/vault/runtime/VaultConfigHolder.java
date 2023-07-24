package io.quarkus.vault.runtime;

import jakarta.inject.Singleton;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

@Singleton
public class VaultConfigHolder {
    VaultRuntimeConfig vaultRuntimeConfig;

    public VaultRuntimeConfig getVaultRuntimeConfig() {
        return vaultRuntimeConfig;
    }

    public VaultConfigHolder setVaultRuntimeConfig(VaultRuntimeConfig vaultRuntimeConfig) {
        this.vaultRuntimeConfig = vaultRuntimeConfig;
        return this;
    }
}

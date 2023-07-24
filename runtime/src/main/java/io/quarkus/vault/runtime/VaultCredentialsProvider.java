package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.DATABASE_DEFAULT_MOUNT;
import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.DEFAULT_REQUEST_PATH;

import java.util.HashMap;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.runtime.config.CredentialsProviderConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

@ApplicationScoped
@Named("vault-credentials-provider")
public class VaultCredentialsProvider implements CredentialsProvider {

    @Inject
    VaultKVSecretEngine vaultKVSecretEngine;
    @Inject
    VaultDynamicCredentialsManager vaultDynamicCredentialsManager;
    @Inject
    VaultConfigHolder vaultConfigHolder;

    @SuppressWarnings("deprecation")
    @Override
    public Map<String, String> getCredentials(String credentialsProviderName) {

        VaultRuntimeConfig vaultConfig = getConfig();
        if (vaultConfig == null) {
            throw new VaultException(
                    "missing Vault configuration required for credentials providers with name " + credentialsProviderName);
        }

        CredentialsProviderConfig config = vaultConfig.credentialsProvider().get(credentialsProviderName);

        if (config == null) {
            throw new VaultException("unknown credentials provider with name " + credentialsProviderName);
        }

        if (config.databaseCredentialsRole().isPresent()) {
            return vaultDynamicCredentialsManager.getDynamicCredentials(DATABASE_DEFAULT_MOUNT, DEFAULT_REQUEST_PATH,
                    config.databaseCredentialsRole().get()).await().indefinitely();
        }

        if (config.credentialsRole().isPresent()) {
            return vaultDynamicCredentialsManager
                    .getDynamicCredentials(config.credentialsMount(), config.credentialsRequestPath(),
                            config.credentialsRole().get())
                    .await().indefinitely();
        }

        if (config.kvPath().isPresent()) {
            String password = vaultKVSecretEngine.readSecret(config.kvPath().get()).get(config.kvKey());
            Map<String, String> result = new HashMap<>();
            result.put(PASSWORD_PROPERTY_NAME, password);
            return result;
        }

        throw new VaultException(
                "one of database-credentials-role or kv-path is required on credentials provider " + credentialsProviderName);
    }

    private VaultRuntimeConfig getConfig() {
        return vaultConfigHolder.getVaultRuntimeConfig();
    }
}

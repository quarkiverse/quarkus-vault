package io.quarkus.vault;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;

/**
 * This service provides access to the system backend.
 *
 * @implNote Wrapper for reactive engine. Request timeouts are accounted for in Vault client.
 * @see VaultRuntimeConfig
 */
@ApplicationScoped
public class VaultSystemBackendEngine {

    private final VaultSystemBackendReactiveEngine engine;

    @Inject
    public VaultSystemBackendEngine(VaultSystemBackendReactiveEngine engine) {
        this.engine = engine;
    }

    /**
     * Initializes a new Vault.
     *
     * @param secretShares specifies the number of shares to split the master key into.
     * @param secretThreshold specifies the number of shares required to reconstruct the master key.
     * @return Vault Init.
     */
    public VaultInit init(int secretShares, int secretThreshold) {
        return engine.init(secretShares, secretThreshold).await().indefinitely();
    }

    /**
     * Check the health status of Vault.
     * Returns Vault health status code only by using HTTP HEAD requests.
     * It is faster than calling {@link #healthStatus() healthStatus()} method which uses HTTP GET to return a complete
     * VaultHealthStatus state.
     *
     * @return Vault Health Status.
     */
    public VaultHealth health() {
        return engine.health().await().indefinitely();
    }

    /**
     * Check and return the health status of Vault.
     * Returns a complete VaultHealthStatus state.
     * This method uses HTTP GET to return a complete state.
     *
     * @return Complete Vault Health Status.
     */
    public VaultHealthStatus healthStatus() {
        return engine.healthStatus().await().indefinitely();
    }

    /**
     * Check the seal status of a Vault.
     *
     * @return Vault Seal Status.
     */
    public VaultSealStatus sealStatus() {
        return engine.sealStatus().await().indefinitely();
    }

    /**
     * Get the rules for the named policy.
     *
     * @param name of the policy
     * @return rules of named policy
     */
    public String getPolicyRules(String name) {
        return engine.getPolicyRules(name).await().indefinitely();
    }

    /**
     * Create or update a policy.
     *
     * @param name policy name
     * @param rules policy content
     */
    public void createUpdatePolicy(String name, String rules) {
        engine.createUpdatePolicy(name, rules).await().indefinitely();
    }

    /**
     * Delete a policy by its name.
     *
     * @param name policy name
     */
    public void deletePolicy(String name) {
        engine.deletePolicy(name).await().indefinitely();
    }

    /**
     * List existing policies.
     *
     * @return a list of all policy names
     */
    public List<String> getPolicies() {
        return engine.getPolicies().await().indefinitely();
    }

    /**
     * Get the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @return current tune info
     */
    public VaultTuneInfo getTuneInfo(String mount) {
        return engine.getTuneInfo(mount).await().indefinitely();
    }

    /**
     * Get the info for a secret engine, including its type.
     *
     * @since Vault 1.10.0
     * @see <a href="https://www.vaultproject.io/api-docs/system/mounts#get-the-configuration-of-a-secret-engine">
     *      Get the configuration of a secret engine
     *      </a>
     *
     * @param mount Name of the secret engine
     * @return current secret engine info
     */
    public VaultSecretEngineInfo getSecretEngineInfo(String mount) {
        return engine.getSecretEngineInfo(mount).await().indefinitely();
    }

    /**
     * Update the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @param tuneInfo Tune info with fields to update
     */
    public void updateTuneInfo(String mount, VaultTuneInfo tuneInfo) {
        engine.updateTuneInfo(mount, tuneInfo).await().indefinitely();
    }

    /**
     * Check if an engine is mounted at a specific mount.
     *
     * @param mount Name of the mount
     * @return True if an engine is mounted, false otherwise
     */
    public boolean isEngineMounted(String mount) {
        return engine.isEngineMounted(mount).await().indefinitely();
    }

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engine Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    public void enable(VaultSecretEngine engine, String mount, String description, EnableEngineOptions options) {
        this.engine.enable(engine, mount, description, options).await().indefinitely();
    }

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engineType Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    public void enable(String engineType, String mount, String description, EnableEngineOptions options) {
        engine.enable(engineType, mount, description, options).await().indefinitely();
    }

    /**
     * Disables the engine at a specific mount.
     *
     * @param mount Engine mount path.
     */
    public void disable(String mount) {
        engine.disable(mount).await().indefinitely();
    }

}

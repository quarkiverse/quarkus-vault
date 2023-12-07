package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import jakarta.annotation.Nullable;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultPluginDetails;
import io.quarkus.vault.sys.VaultPlugins;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;
import io.smallrye.mutiny.Uni;

/**
 * This service provides access to the system backend.
 *
 * @see VaultRuntimeConfig
 */
public interface VaultSystemBackendReactiveEngine {

    /**
     * Initializes a new Vault.
     *
     * @param secretShares specifies the number of shares to split the master key into.
     * @param secretThreshold specifies the number of shares required to reconstruct the master key.
     * @return Vault Init.
     */
    Uni<VaultInit> init(int secretShares, int secretThreshold);

    /**
     * Check the health status of Vault.
     * Returns Vault health status code only by using HTTP HEAD requests.
     * It is faster than calling {@link #healthStatus() healthStatus()} method which uses HTTP GET to return a complete
     * VaultHealthStatus state.
     *
     * @return Vault Health Status.
     */
    Uni<VaultHealth> health();

    /**
     * Check and return the health status of Vault.
     * Returns a complete VaultHealthStatus state.
     * This method uses HTTP GET to return a complete state.
     *
     * @return Complete Vault Health Status.
     */
    Uni<VaultHealthStatus> healthStatus();

    /**
     * Check the seal status of a Vault.
     *
     * @return Vault Seal Status.
     */
    Uni<VaultSealStatus> sealStatus();

    /**
     * Get the rules for the named policy.
     *
     * @param name of the policy
     * @return rules of named policy
     */
    Uni<String> getPolicyRules(String name);

    /**
     * Create or update a policy.
     *
     * @param name policy name
     * @param rules policy content
     */
    Uni<Void> createUpdatePolicy(String name, String rules);

    /**
     * Delete a policy by its name.
     *
     * @param name policy name
     */
    Uni<Void> deletePolicy(String name);

    /**
     * List existing policies.
     *
     * @return a list of all policy names
     */
    Uni<List<String>> getPolicies();

    /**
     * Get the info for a secret engine, including its type.
     *
     * @since Vault 1.10.0
     * @see https://www.vaultproject.io/api-docs/system/mounts#get-the-configuration-of-a-secret-engine
     *
     * @param mount Name of the secret engine
     * @return current secret engine info
     */
    Uni<VaultSecretEngineInfo> getSecretEngineInfo(String mount);

    /**
     * Get the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @return current tune info
     */
    Uni<VaultTuneInfo> getTuneInfo(String mount);

    /**
     * Update the tune info for a secret engine at a specific mount.
     *
     * @param mount Name of the mount
     * @param tuneInfo Tune info with fields to update
     */
    Uni<Void> updateTuneInfo(String mount, VaultTuneInfo tuneInfo);

    /**
     * Check if an engine is mounted at a specific mount.
     *
     * @param mount Name of the mount
     * @return True if an engine is mounted, false otherwise
     */
    Uni<Boolean> isEngineMounted(String mount);

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engine Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    Uni<Void> enable(VaultSecretEngine engine, String mount, String description, EnableEngineOptions options);

    /**
     * Enables a secret engine at a specific mount.
     *
     * @param engineType Type of engine to mount.
     * @param mount Engine mount path.
     * @param description Human friendly description of mount point.
     * @param options Engine options.
     */
    Uni<Void> enable(String engineType, String mount, String description, EnableEngineOptions options);

    /**
     * Disables the engine at a specific mount.
     *
     * @param mount Engine mount path.
     */
    Uni<Void> disable(String mount);

    /**
     * Lists all available plugins.
     *
     * @return Map of plugin types to plugin names.
     */
    Uni<VaultPlugins> listPlugins();

    /**
     * Lists all available plugins of a specific type.
     *
     * @param type Plugin type.
     * @return List of plugin names.
     */
    Uni<List<String>> listPlugins(String type);

    /**
     * Gets details for a specific plugin.
     *
     * @param type Plugin type.
     * @param name Plugin name.
     * @param version Plugin version.
     * @return Plugin info.
     */
    Uni<VaultPluginDetails> getPluginDetails(String type, String name, @Nullable String version);

    /**
     * Registers a plugin.
     *
     * @param type Plugin type.
     * @param name Plugin name.
     * @param version Plugin version.
     * @param sha256 SHA256 of the plugin binary.
     * @param command Plugin command.
     * @param args Plugin args.
     * @param env Plugin environment variables.
     * @return Plugin registration status.
     */
    Uni<Void> registerPlugin(String type, String name, @Nullable String version, String sha256, String command,
            @Nullable List<String> args, @Nullable List<String> env);

    /**
     * Deregisters a plugin.
     *
     * @param type Plugin type.
     * @param name Plugin name.
     * @param version Plugin version.
     */
    Uni<Void> removePlugin(String type, String name, @Nullable String version);

}

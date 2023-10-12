package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.smallrye.mutiny.Uni;

/**
 * This service provides access to the kv secret engine, taking care of authentication,
 * and token extension or renewal, according to ttl and max-ttl.
 *
 * @see VaultRuntimeConfig
 */
public interface VaultKVSecretReactiveEngine {

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     * This is a shortcut to `readSecretJson(String)` when the secret value is a String, which is the common case.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, String>> readSecret(String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path and a mount.
     * This is a shortcut to `readSecretJson(String, String)` when the secret value is a String,
     * which is the common case.
     *
     * @param mount a specific Vault kv mount (it can also be the default configured one)
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, String>> readSecret(String mount, String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, Object>> readSecretJson(String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path
     * and a given mount
     *
     * @param mount the mount to use for the path
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, Object>> readSecretJson(String mount, String path);

    /**
     * Writes the secret at the given path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    Uni<Void> writeSecret(String path, Map<String, String> secret);

    /**
     * Writes the secret at the given path and mount. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param mount a given Vault's Kv mount
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    Uni<Void> writeSecret(String mount, String path, Map<String, String> secret);

    /**
     * Deletes the secret at the given path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param path to delete
     */
    Uni<Void> deleteSecret(String path);

    /**
     * Deletes the secret at the given path for a given Vault's Kv mount.
     * It has no effect if no secret is currently stored at path.
     *
     * @param mount a given Vault's Kv mount
     * @param path to delete
     */
    Uni<Void> deleteSecret(String mount, String path);

    /**
     * List all paths under the specified path.
     *
     * @param path to list
     * @return list of subpaths
     */
    Uni<List<String>> listSecrets(String path);

    /**
     * List all paths under the specified path for a given mount
     *
     * @param mount a given Vault's Kv mount
     * @param path to list
     * @return list of sub-paths
     */
    Uni<List<String>> listSecrets(String mount, String path);
}

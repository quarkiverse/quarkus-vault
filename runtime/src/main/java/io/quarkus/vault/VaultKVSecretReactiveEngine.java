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
     * Provides the values stored in the Vault kv secret engine at a particular path with un-prefixed (default) mount path.
     * This is a shortcut to `readSecretJson(String)` when the secret value is a String, which is the common case.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, String>> readSecret(String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path with prefixed mount path.
     * This is a shortcut to `readSecretJson(String, String)` when the secret value is a String, which is the common case.
     *
     * @param prefix of mount path configured with {@code quarkus.vault.kv-secret-engine-mount-path."prefix"} config property
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     * @throws IllegalArgumentException if mount path for given prefix is not configured
     */
    Uni<Map<String, String>> readSecret(String prefix, String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path with un-prefixed (default) mount path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, Object>> readSecretJson(String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path with prefixed mount path.
     *
     * @param prefix of mount path configured with {@code quarkus.vault.kv-secret-engine-mount-path."prefix"} config property
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     * @throws IllegalArgumentException if mount path for given prefix is not configured
     */
    Uni<Map<String, Object>> readSecretJson(String prefix, String path);

    /**
     * Writes the secret at the given path with un-prefixed (default) mount path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    Uni<Void> writeSecret(String path, Map<String, String> secret);

    /**
     * Writes the secret at the given path with prefixed mount path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param prefix of mount path configured with {@code quarkus.vault.kv-secret-engine-mount-path."prefix"} config property
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     * @throws IllegalArgumentException if mount path for given prefix is not configured
     */
    Uni<Void> writeSecret(String prefix, String path, Map<String, String> secret);

    /**
     * Deletes the secret at the given path with un-prefixed (default) mount path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param path to delete
     */
    Uni<Void> deleteSecret(String path);

    /**
     * Deletes the secret at the given path with prefixed mount path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param prefix of mount path configured with {@code quarkus.vault.kv-secret-engine-mount-path."prefix"} config property
     * @param path to delete
     * @throws IllegalArgumentException if mount path for given prefix is not configured
     */
    Uni<Void> deleteSecret(String prefix, String path);

    /**
     * List all paths under the specified path with un-prefixed (default) mount path.
     *
     * @param path to list
     * @return list of subpaths
     */
    Uni<List<String>> listSecrets(String path);

    /**
     * List all paths under the specified path with prefixed mount path.
     *
     * @param prefix of mount path configured with {@code quarkus.vault.kv-secret-engine-mount-path."prefix"} config property
     * @param path to list
     * @return list of subpaths
     * @throws IllegalArgumentException if mount path for given prefix is not configured
     */
    Uni<List<String>> listSecrets(String prefix, String path);
}

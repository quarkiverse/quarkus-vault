package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;

/**
 * This service provides access to the kv secret engine, taking care of authentication,
 * and token extension or renewal, according to ttl and max-ttl.
 *
 * @see VaultBootstrapConfig
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
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, Object>> readSecretJson(String path);

    /**
     * Writes the secret at the given path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    Uni<Void> writeSecret(String path, Map<String, String> secret);

    /**
     * Deletes the secret at the given path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param path to delete
     */
    Uni<Void> deleteSecret(String path);

    /**
     * List all paths under the specified path.
     *
     * @param path to list
     * @return list of subpaths
     */
    Uni<List<String>> listSecrets(String path);

}

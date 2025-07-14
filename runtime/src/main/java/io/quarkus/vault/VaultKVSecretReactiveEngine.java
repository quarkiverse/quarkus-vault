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
     * @param alias the name of the kv engine mount path, or <default> for the default kv engine
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, String>> readSecret(String alias, String path);

    /**
     * read secret for the default kv engine mount path.
     *
     * @see VaultKVSecretReactiveEngine#readSecret(String, String)
     */
    Uni<Map<String, String>> readSecret(String path);

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param alias the name of the kv engine mount path, or <default> for the default kv engine
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    Uni<Map<String, Object>> readSecretJson(String alias, String path);

    /**
     * read secret as json for the default kv engine mount path.
     *
     * @see VaultKVSecretReactiveEngine#readSecretJson(String, String)
     */
    Uni<Map<String, Object>> readSecretJson(String path);

    /**
     * Writes the secret at the given path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param alias the name of the kv engine mount path, or <default> for the default kv engine
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    Uni<Void> writeSecret(String alias, String path, Map<String, String> secret);

    /**
     * write secret for the default kv engine mount path.
     *
     * @see VaultKVSecretReactiveEngine#writeSecret(String, String, Map)
     */
    Uni<Void> writeSecret(String path, Map<String, String> secret);

    /**
     * Deletes the secret at the given path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param alias the name of the kv engine mount path, or <default> for the default kv engine
     * @param path to delete
     */
    Uni<Void> deleteSecret(String alias, String path);

    /**
     * delete secret for the default kv engine mount path.
     *
     * @see VaultKVSecretReactiveEngine#deleteSecret(String, String)
     */
    Uni<Void> deleteSecret(String path);

    /**
     * List all paths under the specified path.
     *
     * @param alias the name of the kv engine mount path, or <default> for the default kv engine
     * @param path to list
     * @return list of subpaths
     */
    Uni<List<String>> listSecrets(String alias, String path);

    /**
     * List all paths under the specified path for the default kv engine mount path.
     *
     * @see VaultKVSecretReactiveEngine#listSecrets(String, String)
     */
    Uni<List<String>> listSecrets(String path);
}

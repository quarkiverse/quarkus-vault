package io.quarkus.vault;

import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * This service provides access to the kv secret engine, taking care of authentication,
 * and token extension or renewal, according to ttl and max-ttl.
 *
 * @implNote Wrapper for reactive engine. Request timeouts are accounted for in Vault client.
 * @see VaultRuntimeConfig
 */
@ApplicationScoped
public class VaultKVSecretEngine {

    private final VaultKVSecretReactiveEngine engine;

    @Inject
    public VaultKVSecretEngine(VaultKVSecretReactiveEngine engine) {
        this.engine = engine;
    }

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     * This is a shortcut to `readSecretJson(String)` when the secret value is a String, which is the common case.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    public Map<String, String> readSecret(String path) {
        return engine.readSecret(path).await().indefinitely();
    }

    /**
     * Provides the values stored in the Vault kv secret engine at a particular path.
     *
     * @param path in Vault, without the kv engine mount path
     * @return list of key value pairs stored at 'path' in Vault
     */
    public Map<String, Object> readSecretJson(String path) {
        return engine.readSecretJson(path).await().indefinitely();
    }

    /**
     * Writes the secret at the given path. If the path does not exist, the secret will
     * be created. If not the new secret will be merged with the existing one.
     *
     * @param path in Vault, without the kv engine mount path
     * @param secret to write at path
     */
    public void writeSecret(String path, Map<String, String> secret) {
        engine.writeSecret(path, secret).await().indefinitely();
    }

    /**
     * Deletes the secret at the given path. It has no effect if no secret is currently
     * stored at path.
     *
     * @param path to delete
     */
    public void deleteSecret(String path) {
        engine.deleteSecret(path).await().indefinitely();
    }

    /**
     * Permanently deletes the specified secret versions at the given path. Destroyed versions cannot
     * be undeleted. This operation is supported by kv version 2 secret engines only, and throws an
     * {@link UnsupportedOperationException} for a kv version 1 secret engine.
     *
     * @param path in Vault, without the kv engine mount path
     * @param versions the secret versions to destroy
     */
    public void destroySecret(String path, List<Integer> versions) {
        engine.destroySecret(path, versions).await().indefinitely();
    }

    /**
     * List all paths under the specified path.
     *
     * @param path to list
     * @return list of subpaths
     */
    public List<String> listSecrets(String path) {
        return engine.listSecrets(path).await().indefinitely();
    }

}

package io.quarkus.vault;

/**
 * Allows obtaining Kv engines for specific mount paths.
 *
 * @see VaultKVSecretEngine
 */
public interface VaultKvSecretEngineFactory {

    /**
     * Get a PKI engine for a specific mount.
     *
     * @param mount Engine mount path.
     * @return Kv engine interface.
     */
    VaultKVSecretEngine engine(String mount);

    /**
     * Get a PKI reactive engine for a specific mount.
     *
     * @param mount Engine mount path.
     * @return Kv engine interface.
     */
    VaultKVSecretReactiveEngine reactiveEngine(String mount);
}

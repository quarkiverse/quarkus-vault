package io.quarkus.vault;

/**
 * Allows obtaining PKI engines for specific mount paths.
 *
 * @see VaultPKISecretEngine
 */
public interface VaultPKISecretEngineFactory {

    /**
     * Get a PKI engine for a specific mount.
     *
     * @param mount Engine mount path.
     *
     * @return PKI engine interface.
     */
    VaultPKISecretEngine engine(String mount);

    /**
     * Get a PKI reactive engine for a specific mount.
     *
     * @param mount Engine mount path.
     *
     * @return PKI engine interface.
     */
    VaultPKISecretReactiveEngine reactiveEngine(String mount);

}

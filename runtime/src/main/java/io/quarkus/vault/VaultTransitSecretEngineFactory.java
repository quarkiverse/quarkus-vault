package io.quarkus.vault;

/**
 * Allows obtaining Transit engines for specfic mount paths
 *
 * @see VaultTransitSecretEngine
 */
public interface VaultTransitSecretEngineFactory {

    /**
     * Get a Transit engine for a specific mount
     *
     * @param mount Transit engine mount path
     * @return [VaultTransitSecretEngine]
     */
    VaultTransitSecretEngine engine(String mount);

    /**
     * Get a Transit reactive engine for a specific mount
     *
     * @param mount Transit engine mount path
     * @return [VaultTransitSecretReactiveEngine]
     */
    VaultTransitSecretReactiveEngine reactiveEngine(String mount);
}

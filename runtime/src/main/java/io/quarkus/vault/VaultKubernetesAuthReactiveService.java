package io.quarkus.vault;

import java.util.List;

import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;
import io.smallrye.mutiny.Uni;

/**
 * This service provides programmatic access to the Kubernetes auth method.
 * This may be used by admin clients that provision Vault for use from Kubernetes.
 */
public interface VaultKubernetesAuthReactiveService {

    /**
     * Configure the Kubernetes auth method.
     *
     * @param config configuration detail
     */
    Uni<Void> configure(VaultKubernetesAuthConfig config);

    /**
     * Gives access to the currently configured Kubernetes auth method.
     *
     * @return the configuration
     */
    Uni<VaultKubernetesAuthConfig> getConfig();

    /**
     * Returns the definition of a Kubernetes vault role.
     *
     * @param name role name
     * @return the Kubernetes vault role
     */
    Uni<VaultKubernetesAuthRole> getRole(String name);

    /**
     * Create or update a Kubernetes vault role.
     *
     * @param name role name
     * @param role role attributes
     */
    Uni<Void> createRole(String name, VaultKubernetesAuthRole role);

    /**
     * Delete a Kubernetes vault role through its name.
     *
     * @param name role name to delete
     */
    Uni<Void> deleteRole(String name);

    /**
     * Get the names of the existing Kubernetes vault roles.
     *
     * @return the role names
     */
    Uni<List<String>> getRoles();

}

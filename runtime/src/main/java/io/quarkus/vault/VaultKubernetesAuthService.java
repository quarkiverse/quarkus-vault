package io.quarkus.vault;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;

/**
 * This service provides programmatic access to the Kubernetes auth method.
 * This may be used by admin clients that provision Vault for use from Kubernetes.
 *
 * @implNote Wrapper for reactive service. Request timeouts are accounted for in Vault client.
 */
@ApplicationScoped
public class VaultKubernetesAuthService {

    private final VaultKubernetesAuthReactiveService service;

    @Inject
    public VaultKubernetesAuthService(VaultKubernetesAuthReactiveService service) {
        this.service = service;
    }

    /**
     * Configure the Kubernetes auth method.
     *
     * @param config configuration detail
     */
    public void configure(VaultKubernetesAuthConfig config) {
        service.configure(config).await().indefinitely();
    }

    /**
     * Gives access to the currently configured Kubernetes auth method.
     *
     * @return the configuration
     */
    public VaultKubernetesAuthConfig getConfig() {
        return service.getConfig().await().indefinitely();
    }

    /**
     * Returns the definition of a Kubernetes vault role.
     *
     * @param name role name
     * @return the Kubernetes vault role
     */
    public VaultKubernetesAuthRole getRole(String name) {
        return service.getRole(name).await().indefinitely();
    }

    /**
     * Create or update a Kubernetes vault role.
     *
     * @param name role name
     * @param role role attributes
     */
    public void createRole(String name, VaultKubernetesAuthRole role) {
        service.createRole(name, role).await().indefinitely();
    }

    /**
     * Delete a Kubernetes vault role through its name.
     *
     * @param name role name to delete
     */
    public void deleteRole(String name) {
        service.deleteRole(name).await().indefinitely();
    }

    /**
     * Get the names of the existing Kubernetes vault roles.
     *
     * @return the role names
     */
    public List<String> getRoles() {
        return service.getRoles().await().indefinitely();
    }

}

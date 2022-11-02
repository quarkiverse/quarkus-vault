package io.quarkus.vault;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.quarkus.vault.runtime.client.VaultClientException;

/**
 * This service provides programmatic access to the AppRole auth method.
 * This may be used by admin clients that provision Vault for use from Kubernetes.
 *
 * @implNote Wrapper for reactive service. Request timeouts are accounted for in Vault client.
 */
@ApplicationScoped
public class VaultAppRoleAuthService {

    private final VaultAppRoleAuthReactiveService service;

    @Inject
    public VaultAppRoleAuthService(VaultAppRoleAuthReactiveService service) {
        this.service = service;
    }

    /**
     * Get the names of the existing AppRole vault roles.
     *
     * @return the role names
     */
    public List<String> getAppRoles() {
        return service.getAppRoles().await().indefinitely();
    }

    /**
     * Create or update a vault appRole.
     *
     * @param name appRole name
     * @param appRole appRole attributes
     */
    public void createOrUpdateAppRole(String name, VaultAppRoleAuthRole appRole) {
        service.createOrUpdateAppRole(name, appRole).await().indefinitely();
    }

    /**
     * Delete a vault appRole through its name.
     *
     * @param name appRole name to delete
     */
    public void deleteAppRole(String name) {
        service.deleteAppRole(name).await().indefinitely();
    }

    /**
     * Returns the definition of a vault appRole.
     *
     * @param name appRole name
     * @return the vault appRole
     */
    public VaultAppRoleAuthRole getAppRole(String name) {
        return service.getAppRole(name).await().indefinitely();
    }

    /**
     * Get vault approle role id.
     *
     * @param name appRole name
     * @return the appRole role Id
     */
    public String getAppRoleRoleId(String name) {
        return service.getAppRoleRoleId(name).await().indefinitely();
    }

    /**
     * Set vault approle role id.
     *
     * @param name appRole name
     * @param roleId appRole roleId
     * @return the new role Id
     */
    public void setAppRoleRoleId(String name, String roleId) {
        service.setAppRoleRoleId(name, roleId).await().indefinitely();
    }

    /**
     * Generate a new secretId for vault appRole with given name.
     *
     * @param name appRole name
     * @param newSecretIdRequest new secretId attributes
     * @return the newly created secretId
     */
    public VaultAppRoleSecretId createNewSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        return service.createNewSecretId(name, newSecretIdRequest).await().indefinitely();
    }

    /**
     * Create a custom secretId for vault appRole with given name.
     *
     * @param name appRole name
     * @param newSecretIdRequest new secretId attributes
     * @return the newly created secretId
     */
    public VaultAppRoleSecretId createCustomSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        return service.createCustomSecretId(name, newSecretIdRequest).await().indefinitely();
    }

    /**
     * Get the keys of existing AppRole secretId accessors for vault appRole with given name.
     *
     * @param name appRole name
     * @return the secretId accessors keys
     */
    public List<String> getSecretIdAccessors(String name) {
        return service.getSecretIdAccessors(name).await().indefinitely();
    }

    /**
     * Get AppRole secretId accessor for vault appRole with given name and secret accessor id.
     *
     * @param name the name appRole name
     * @param accessorId the secret accessor id
     * @return the SecretId accessor details
     */
    public VaultAppRoleSecretIdAccessor getSecretIdAccessor(String name, String accessorId) {
        return service.getSecretIdAccessor(name, accessorId).await().indefinitely();
    }

    /**
     * Delete AppRole secretId accessor for given vault appRole name and secret accessor id.
     *
     * @param name the name appRole name
     * @param accessorId the secret accessor id
     */
    public void deleteSecretIdAccessor(String name, String accessorId) {
        service.deleteSecretIdAccessor(name, accessorId).await().indefinitely();
    }

    /**
     * Get AppRole secretId for vault appRole with given name and secret id.
     *
     * @param name the name appRole name
     * @param secretId the secret id
     * @return the SecretId accessor details
     */
    public VaultAppRoleSecretIdAccessor getSecretId(String name, String secretId) {
        try {
            return service.getSecretId(name, secretId).await().indefinitely();
        } catch (VaultClientException e) {
            if (e.getStatus() == 204) {
                // secretId not found
                return null;
            }
            throw e;
        }
    }

    /**
     * Delete AppRole secretId for given vault appRole name and secret Id.
     *
     * @param name the name appRole name
     * @param secretId the secret id
     */
    public void deleteSecretId(String name, String secretId) {
        service.deleteSecretId(name, secretId).await().indefinitely();
    }

}

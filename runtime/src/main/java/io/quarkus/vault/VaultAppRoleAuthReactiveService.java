package io.quarkus.vault;

import java.util.List;

import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.smallrye.mutiny.Uni;

/**
 * This service provides programmatic access to the AppRole auth method.
 * This may be used by admin clients that provision Vault for use from outside Kubernetes.
 */
public interface VaultAppRoleAuthReactiveService {

    /**
     * Get the names of the existing AppRole vault roles.
     *
     * @return the role names
     */
    Uni<List<String>> getAppRoles();

    /**
     * Create or update a vault appRole.
     *
     * @param name appRole name
     * @param appRole appRole attributes
     */
    Uni<Void> createOrUpdateAppRole(String name, VaultAppRoleAuthRole appRole);

    /**
     * Delete a vault appRole through its name.
     *
     * @param name appRole name to delete
     */
    Uni<Void> deleteAppRole(String name);

    /**
     * Returns the definition of a vault appRole.
     *
     * @param name appRole name
     * @return the vault appRole
     */
    Uni<VaultAppRoleAuthRole> getAppRole(String name);

    /**
     * Get vault approle role id.
     *
     * @param name appRole name
     * @return the appRole role Id
     */
    Uni<String> getAppRoleRoleId(String name);

    /**
     * Set vault approle role id.
     *
     * @param name appRole name
     * @param roleId appRole roleId
     */
    Uni<Void> setAppRoleRoleId(String name, String roleId);

    /**
     * Generate a new secretId for vault appRole with given name.
     *
     * @param name appRole name
     * @param newSecretIdRequest new secretId attributes
     * @return the newly created secretId
     */
    Uni<VaultAppRoleSecretId> createNewSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest);

    /**
     * Create a custom secretId for vault appRole with given name.
     *
     * @param name appRole name
     * @param newSecretIdRequest new secretId attributes
     * @return the newly created secretId
     */
    Uni<VaultAppRoleSecretId> createCustomSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest);

    /**
     * Get the keys of existing AppRole secretId accessors for vault appRole with given name.
     *
     * @param name the name appRole name
     * @return the secretId accessors keys
     */
    Uni<List<String>> getSecretIdAccessors(String name);

    /**
     * Get AppRole secretId accessor for vault appRole with given name and secret accessor id.
     *
     * @param name the name appRole name
     * @param accessorId the secret accessor id
     * @return the SecretId accessor details
     */
    Uni<VaultAppRoleSecretIdAccessor> getSecretIdAccessor(String name, String accessorId);

    /**
     * Delete AppRole secretId accessor for given vault appRole name and secret accessor id.
     *
     * @param name the name appRole name
     * @param accessorId the secret accessor id
     */
    Uni<Void> deleteSecretIdAccessor(String name, String accessorId);

    /**
     * Get AppRole secretId for vault appRole with given name and secret id.
     *
     * @param name the name appRole name
     * @param secretId the secret id
     * @return the SecretId accessor details
     */
    Uni<VaultAppRoleSecretIdAccessor> getSecretId(String name, String secretId);

    /**
     * Delete AppRole secretId for given vault appRole name and secret Id.
     *
     * @param name the name appRole name
     * @param secretId the secret id
     */
    Uni<Void> deleteSecretId(String name, String secretId);

}

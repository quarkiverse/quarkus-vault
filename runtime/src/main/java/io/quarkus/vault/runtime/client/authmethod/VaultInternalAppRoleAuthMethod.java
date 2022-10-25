package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthCreateCustomSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthCreateSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthListRolesResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthListSecretIdAccessorsResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadRoleResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdAccessorData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdAccessorResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthRoleData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthRoleIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthSetRoleIdResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleGenerateNewSecretID;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalAppRoleAuthMethod extends VaultInternalBase {
    @Inject
    private VaultConfigHolder vaultConfigHolder;

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (approle)]";
    }

    public Uni<VaultAppRoleAuth> login(VaultClient vaultClient, String roleId, String secretId) {
        VaultAppRoleAuthBody body = new VaultAppRoleAuthBody(roleId, secretId);
        return vaultClient.post(opName("Login"), getAppRoleAuthMountPath() + "/login", null, body, VaultAppRoleAuth.class);
    }

    public Uni<Void> createAuthRole(VaultClient vaultClient, String token, String name, VaultAppRoleAuthRoleData body) {
        return vaultClient.post(opName("Create Role"), getAppRoleAuthMountPath() + "/role/" + name, token, body, 204);
    }

    private String getAppRoleAuthMountPath() {
        return vaultConfigHolder.getVaultBootstrapConfig().authentication.appRole.authMountPath;
    }

    public Uni<VaultAppRoleAuthReadRoleResult> getVaultAuthRole(VaultClient vaultClient, String token, String name) {
        return vaultClient.get(opName("Get Role"), getAppRoleAuthMountPath() + "/role/" + name, token,
                VaultAppRoleAuthReadRoleResult.class);
    }

    public Uni<VaultAppRoleAuthListRolesResult> listAuthRoles(VaultClient vaultClient, String token) {
        return vaultClient.list(opName("List Roles"), getAppRoleAuthMountPath() + "/role", token,
                VaultAppRoleAuthListRolesResult.class);
    }

    public Uni<Void> deleteAuthRoles(VaultClient vaultClient, String token, String name) {
        return vaultClient.delete(opName("Delete Role"), getAppRoleAuthMountPath() + "/role/" + name, token, 204);
    }

    public Uni<VaultAppRoleAuthSetRoleIdResult> getRoleId(VaultClient vaultClient, String token, String name) {
        return vaultClient.get(opName("Get Role ID"), getAppRoleAuthMountPath() + "/role/" + name + "/role-id", token,
                VaultAppRoleAuthSetRoleIdResult.class);
    }

    public Uni<Void> setRoleId(VaultClient vaultClient, String token, String name,
            VaultAppRoleAuthRoleIdData body) {
        return vaultClient.post(opName("Set Role ID"), getAppRoleAuthMountPath() + "/role/" + name + "/role-id", token, body,
                204);
    }

    public Uni<VaultAppRoleGenerateNewSecretID> createSecretId(VaultClient vaultClient, String token, String name,
            VaultAppRoleAuthCreateSecretIdData body) {
        return vaultClient.post(opName("Create new secret ID"), getAppRoleAuthMountPath() + "/role/" + name + "/secret-id",
                token, body,
                VaultAppRoleGenerateNewSecretID.class);
    }

    public Uni<VaultAppRoleAuthReadSecretIdResult> readSecretId(VaultClient vaultClient, String token, String name,
            VaultAppRoleAuthReadSecretIdData body) {
        return vaultClient.post(opName("Read secret ID"), getAppRoleAuthMountPath() + "/role/" + name + "/secret-id/lookup",
                token, body,
                VaultAppRoleAuthReadSecretIdResult.class);
    }

    public Uni<Void> deleteSecretId(VaultClient vaultClient, String token, String name, VaultAppRoleAuthReadSecretIdData body) {
        return vaultClient.post(opName("Delete secret ID"), getAppRoleAuthMountPath() + "/role/" + name + "/secret-id/destroy",
                token, body, 204);
    }

    public Uni<VaultAppRoleGenerateNewSecretID> createCustomSecretId(VaultClient vaultClient, String token, String name,
            VaultAppRoleAuthCreateCustomSecretIdData body) {
        return vaultClient.post(opName("Create new secret ID"),
                getAppRoleAuthMountPath() + "/role/" + name + "/custom-secret-id", token, body,
                VaultAppRoleGenerateNewSecretID.class);
    }

    public Uni<VaultAppRoleAuthListSecretIdAccessorsResult> listSecretIdAccessors(VaultClient vaultClient, String token,
            String name) {
        return vaultClient.list(opName("List secret ID accessors"), getAppRoleAuthMountPath() + "/role/" + name + "/secret-id",
                token,
                VaultAppRoleAuthListSecretIdAccessorsResult.class);
    }

    public Uni<VaultAppRoleAuthReadSecretIdAccessorResult> readSecretIdAccessor(VaultClient vaultClient, String token,
            String name, VaultAppRoleAuthReadSecretIdAccessorData body) {
        return vaultClient.list(opName("Read secret ID accessor"),
                getAppRoleAuthMountPath() + "/role/" + name + "/secret-id-accessor/lookup", token,
                VaultAppRoleAuthReadSecretIdAccessorResult.class);
    }

    public Uni<Void> deleteSecretIdAccessor(VaultClient vaultClient, String token, String name,
            VaultAppRoleAuthReadSecretIdAccessorData body) {
        return vaultClient.post(opName("Delete secret ID accessor"),
                getAppRoleAuthMountPath() + "/role/" + name + "/secret-id-accessor/destroy", token, body, 204);
    }

}

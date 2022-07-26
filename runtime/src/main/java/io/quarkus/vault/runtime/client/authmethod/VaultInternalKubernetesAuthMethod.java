package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthBody;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthListRolesResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthReadRoleResult;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalKubernetesAuthMethod extends VaultInternalBase {

    @Inject
    private VaultConfigHolder vaultConfigHolder;

    private String getKubernetesAuthMountPath() {
        return vaultConfigHolder.getVaultBootstrapConfig().authentication.kubernetes.authMountPath;
    }

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [AUTH (k8s)]";
    }

    public Uni<VaultKubernetesAuth> login(VaultClient vaultClient, String role, String jwt) {
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        return vaultClient.post(opName("Login"), getKubernetesAuthMountPath() + "/login", null, body,
                VaultKubernetesAuth.class);
    }

    public Uni<Void> createAuthRole(VaultClient vaultClient, String token, String name, VaultKubernetesAuthRoleData body) {
        return vaultClient.post(opName("Create Role"), getKubernetesAuthMountPath() + "/role/" + name, token, body, 204);
    }

    public Uni<VaultKubernetesAuthReadRoleResult> getVaultAuthRole(VaultClient vaultClient, String token, String name) {
        return vaultClient.get(opName("Get Role"), getKubernetesAuthMountPath() + "/role/" + name, token,
                VaultKubernetesAuthReadRoleResult.class);
    }

    public Uni<VaultKubernetesAuthListRolesResult> listAuthRoles(VaultClient vaultClient, String token) {
        return vaultClient.list(opName("List Roles"), getKubernetesAuthMountPath() + "/role", token,
                VaultKubernetesAuthListRolesResult.class);
    }

    public Uni<Void> deleteAuthRoles(VaultClient vaultClient, String token, String name) {
        return vaultClient.delete(opName("Delete Role"), getKubernetesAuthMountPath() + "/role/" + name, token, 204);
    }

    public Uni<Void> configureAuth(VaultClient vaultClient, String token, VaultKubernetesAuthConfigData config) {
        return vaultClient.post(opName("Configure"), getKubernetesAuthMountPath() + "/config", token, config, 204);
    }

    public Uni<VaultKubernetesAuthConfigResult> readAuthConfig(VaultClient vaultClient, String token) {
        return vaultClient.get(opName("Read Configuration"), getKubernetesAuthMountPath() + "/config", token,
                VaultKubernetesAuthConfigResult.class);
    }
}

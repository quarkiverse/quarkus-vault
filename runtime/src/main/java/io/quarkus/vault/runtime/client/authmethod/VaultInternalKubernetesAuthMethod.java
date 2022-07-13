package io.quarkus.vault.runtime.client.authmethod;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.quarkus.vault.runtime.VaultConfigHolder;
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

    public Uni<VaultKubernetesAuth> login(String role, String jwt) {
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        return vaultClient.post(getKubernetesAuthMountPath() + "/login", null, body, VaultKubernetesAuth.class);
    }

    public Uni<Void> createAuthRole(String token, String name, VaultKubernetesAuthRoleData body) {
        return vaultClient.post(getKubernetesAuthMountPath() + "/role/" + name, token, body, 204);
    }

    public Uni<VaultKubernetesAuthReadRoleResult> getVaultAuthRole(String token, String name) {
        return vaultClient.get(getKubernetesAuthMountPath() + "/role/" + name, token, VaultKubernetesAuthReadRoleResult.class);
    }

    public Uni<VaultKubernetesAuthListRolesResult> listAuthRoles(String token) {
        return vaultClient.list(getKubernetesAuthMountPath() + "/role", token, VaultKubernetesAuthListRolesResult.class);
    }

    public Uni<Void> deleteAuthRoles(String token, String name) {
        return vaultClient.delete(getKubernetesAuthMountPath() + "/role/" + name, token, 204);
    }

    public Uni<Void> configureAuth(String token, VaultKubernetesAuthConfigData config) {
        return vaultClient.post(getKubernetesAuthMountPath() + "/config", token, config, 204);
    }

    public Uni<VaultKubernetesAuthConfigResult> readAuthConfig(String token) {
        return vaultClient.get(getKubernetesAuthMountPath() + "/config", token, VaultKubernetesAuthConfigResult.class);
    }

    public VaultInternalKubernetesAuthMethod setVaultConfigHolder(VaultConfigHolder vaultConfigHolder) {
        this.vaultConfigHolder = vaultConfigHolder;
        return this;
    }
}

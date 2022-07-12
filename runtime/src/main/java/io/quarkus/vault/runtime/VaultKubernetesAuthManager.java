package io.quarkus.vault.runtime;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultKubernetesAuthReactiveService;
import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalKubernetesAuthMethod;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthConfigData;
import io.quarkus.vault.runtime.client.dto.auth.VaultKubernetesAuthRoleData;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKubernetesAuthManager implements VaultKubernetesAuthReactiveService {

    @Inject
    private VaultClient vaultClient;
    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalKubernetesAuthMethod vaultInternalKubernetesAuthMethod;

    @Override
    public Uni<Void> configure(VaultKubernetesAuthConfig config) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.configureAuth(vaultClient, token, new VaultKubernetesAuthConfigData()
                    .setIssuer(config.issuer)
                    .setKubernetesCaCert(config.kubernetesCaCert)
                    .setKubernetesHost(config.kubernetesHost)
                    .setPemKeys(config.pemKeys)
                    .setTokenReviewerJwt(config.tokenReviewerJwt));
        });
    }

    @Override
    public Uni<VaultKubernetesAuthConfig> getConfig() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.readAuthConfig(vaultClient, token)
                    .map(result -> new VaultKubernetesAuthConfig()
                            .setKubernetesCaCert(result.data.kubernetesCaCert)
                            .setKubernetesHost(result.data.kubernetesHost)
                            .setIssuer(result.data.issuer)
                            .setPemKeys(result.data.pemKeys)
                            .setTokenReviewerJwt(result.data.tokenReviewerJwt));
        });
    }

    public Uni<VaultKubernetesAuthRole> getRole(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.getVaultAuthRole(vaultClient, token, name)
                    .map(result -> {
                        VaultKubernetesAuthRoleData role = result.data;
                        return new VaultKubernetesAuthRole()
                                .setBoundServiceAccountNames(role.boundServiceAccountNames)
                                .setBoundServiceAccountNamespaces(role.boundServiceAccountNamespaces)
                                .setAudience(role.audience)
                                .setTokenTtl(role.tokenTtl)
                                .setTokenMaxTtl(role.tokenMaxTtl)
                                .setTokenPolicies(role.tokenPolicies)
                                .setTokenBoundCidrs(role.tokenBoundCidrs)
                                .setTokenExplicitMaxTtl(role.tokenExplicitMaxTtl)
                                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                                .setTokenNumUses(role.tokenNumUses)
                                .setTokenPeriod(role.tokenPeriod)
                                .setTokenType(role.tokenType);
                    });
        });
    }

    public Uni<Void> createRole(String name, VaultKubernetesAuthRole role) {
        VaultKubernetesAuthRoleData body = new VaultKubernetesAuthRoleData()
                .setBoundServiceAccountNames(role.boundServiceAccountNames)
                .setBoundServiceAccountNamespaces(role.boundServiceAccountNamespaces)
                .setAudience(role.audience)
                .setTokenTtl(role.tokenTtl)
                .setTokenMaxTtl(role.tokenMaxTtl)
                .setTokenPolicies(role.tokenPolicies)
                .setTokenBoundCidrs(role.tokenBoundCidrs)
                .setTokenExplicitMaxTtl(role.tokenExplicitMaxTtl)
                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                .setTokenNumUses(role.tokenNumUses)
                .setTokenPeriod(role.tokenPeriod)
                .setTokenType(role.tokenType);
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.createAuthRole(vaultClient, token, name, body);
        });
    }

    @Override
    public Uni<List<String>> getRoles() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.listAuthRoles(vaultClient, token)
                    .map(r -> r.data.keys)
                    .onFailure(VaultClientException.class).recoverWithUni(e -> {
                        if (((VaultClientException) e).getStatus() == 404) {
                            return Uni.createFrom().item(Collections.emptyList());
                        } else {
                            return Uni.createFrom().failure(e);
                        }
                    });
        });
    }

    @Override
    public Uni<Void> deleteRole(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalKubernetesAuthMethod.deleteAuthRoles(vaultClient, token, name);
        });
    }
}

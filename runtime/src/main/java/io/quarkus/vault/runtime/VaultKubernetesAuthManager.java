package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.DurationHelper.fromVaultDuration;
import static io.quarkus.vault.runtime.DurationHelper.toDurationSeconds;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultKubernetesAuthReactiveService;
import io.quarkus.vault.auth.VaultKubernetesAuthConfig;
import io.quarkus.vault.auth.VaultKubernetesAuthRole;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetes;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetesConfigureParams;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetesUpdateRoleParams;
import io.quarkus.vault.client.api.common.VaultTokenType;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultKubernetesAuthManager implements VaultKubernetesAuthReactiveService {

    private final VaultAuthKubernetes k8s;

    @Inject
    public VaultKubernetesAuthManager(VaultClient client, VaultConfigHolder configHolder) {
        this.k8s = client.auth().kubernetes(configHolder.getVaultRuntimeConfig().authentication().kubernetes().authMountPath());
    }

    @Override
    public Uni<Void> configure(VaultKubernetesAuthConfig config) {
        var params = new VaultAuthKubernetesConfigureParams()
                .setIssuer(config.issuer)
                .setKubernetesCaCert(config.kubernetesCaCert)
                .setKubernetesHost(config.kubernetesHost)
                .setPemKeys(config.pemKeys)
                .setTokenReviewerJwt(config.tokenReviewerJwt);
        return Uni.createFrom().completionStage(k8s.configure(params));
    }

    @Override
    public Uni<VaultKubernetesAuthConfig> getConfig() {
        return Uni.createFrom().completionStage(k8s.readConfig())
                .map(result -> new VaultKubernetesAuthConfig()
                        .setKubernetesCaCert(result.getKubernetesCaCert())
                        .setKubernetesHost(result.getKubernetesHost())
                        .setIssuer(result.getIssuer())
                        .setPemKeys(result.getPemKeys())
                        .setTokenReviewerJwt(result.getTokenReviewerJwt()));
    }

    public Uni<VaultKubernetesAuthRole> getRole(String name) {
        return Uni.createFrom().completionStage(k8s.readRole(name))
                .map(result -> new VaultKubernetesAuthRole()
                        .setBoundServiceAccountNames(result.getBoundServiceAccountNames())
                        .setBoundServiceAccountNamespaces(result.getBoundServiceAccountNamespaces())
                        .setAudience(result.getAudience())
                        .setTokenTtl(toDurationSeconds(result.getTokenTtl()))
                        .setTokenMaxTtl(toDurationSeconds(result.getTokenMaxTtl()))
                        .setTokenPolicies(result.getTokenPolicies())
                        .setTokenBoundCidrs(result.getTokenBoundCidrs())
                        .setTokenExplicitMaxTtl(toDurationSeconds(result.getTokenExplicitMaxTtl()))
                        .setTokenNoDefaultPolicy(result.isTokenNoDefaultPolicy())
                        .setTokenNumUses(result.getTokenNumUses())
                        .setTokenPeriod(toDurationSeconds(result.getTokenPeriod()))
                        .setTokenType(result.getTokenType() != null ? result.getTokenType().getValue() : null));
    }

    public Uni<Void> createRole(String name, VaultKubernetesAuthRole role) {
        var params = new VaultAuthKubernetesUpdateRoleParams()
                .setBoundServiceAccountNames(role.boundServiceAccountNames)
                .setBoundServiceAccountNamespaces(role.boundServiceAccountNamespaces)
                .setAudience(role.audience)
                .setTokenTtl(fromVaultDuration(role.tokenTtl))
                .setTokenMaxTtl(fromVaultDuration(role.tokenMaxTtl))
                .setTokenPolicies(role.tokenPolicies)
                .setTokenBoundCidrs(role.tokenBoundCidrs)
                .setTokenExplicitMaxTtl(fromVaultDuration(role.tokenExplicitMaxTtl))
                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                .setTokenNumUses(role.tokenNumUses)
                .setTokenPeriod(fromVaultDuration(role.tokenPeriod))
                .setTokenType(VaultTokenType.from(role.tokenType));
        return Uni.createFrom().completionStage(k8s.updateRole(name, params));
    }

    @Override
    public Uni<List<String>> getRoles() {
        return Uni.createFrom().completionStage(k8s.listRoles());
    }

    @Override
    public Uni<Void> deleteRole(String name) {
        return Uni.createFrom().completionStage(k8s.deleteRole(name));
    }
}

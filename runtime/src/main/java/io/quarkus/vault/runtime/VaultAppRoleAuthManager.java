package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.DurationHelper.*;

import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultAppRoleAuthReactiveService;
import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRole;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleCreateCustomSecretIdParams;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdParams;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleUpdateRoleParams;
import io.quarkus.vault.client.api.common.VaultTokenType;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultAppRoleAuthManager implements VaultAppRoleAuthReactiveService {

    private final VaultAuthAppRole appRole;

    @Inject
    public VaultAppRoleAuthManager(VaultClient client, VaultConfigHolder configHolder) {
        this.appRole = client.auth().appRole(configHolder.getVaultRuntimeConfig().authentication().appRole().authMountPath());
    }

    @Override
    public Uni<List<String>> getAppRoles() {
        return appRole.listRoles();
    }

    @Override
    public Uni<Void> createOrUpdateAppRole(String name, VaultAppRoleAuthRole role) {
        var params = new VaultAuthAppRoleUpdateRoleParams()
                .setBindSecretId(role.bindSecretId)
                .setSecretIdBoundCidrs(role.secretIdBoundCidrs)
                .setSecretIdNumUses(role.secretIdNumUses)
                .setSecretIdTtl(fromVaultDuration(role.secretIdTtl))
                .setLocalSecretIds(role.localSecretIds)
                .setTokenTtl(fromVaultDuration(role.tokenTtl))
                .setTokenMaxTtl(fromVaultDuration(role.tokenMaxTtl))
                .setTokenPolicies(role.tokenPolicies)
                .setTokenBoundCidrs(role.tokenBoundCidrs)
                .setTokenExplicitMaxTtl(fromVaultDuration(role.tokenExplicitMaxTtl))
                .setTokenNoDefaultPolicy(role.tokenNoDefaultPolicy)
                .setTokenNumUses(role.tokenNumUses)
                .setTokenPeriod(fromVaultDuration(role.tokenPeriod))
                .setTokenType(VaultTokenType.from(role.tokenType));
        return appRole.updateRole(name, params);
    }

    @Override
    public Uni<Void> deleteAppRole(String name) {
        return appRole.deleteRole(name);
    }

    @Override
    public Uni<VaultAppRoleAuthRole> getAppRole(String name) {
        return appRole.readRole(name)
                .map(role -> new VaultAppRoleAuthRole()
                        .setBindSecretId(role.isBindSecretId())
                        .setSecretIdBoundCidrs(role.getSecretIdBoundCidrs())
                        .setSecretIdNumUses(role.getSecretIdNumUses())
                        .setSecretIdTtl(toStringDurationSeconds(role.getSecretIdTtl()))
                        .setLocalSecretIds(role.isLocalSecretIds())
                        .setTokenTtl(toDurationSeconds(role.getTokenTtl()))
                        .setTokenMaxTtl(toDurationSeconds(role.getTokenMaxTtl()))
                        .setTokenPolicies(role.getTokenPolicies())
                        .setTokenBoundCidrs(role.getTokenBoundCidrs())
                        .setTokenExplicitMaxTtl(toDurationSeconds(role.getTokenExplicitMaxTtl()))
                        .setTokenNoDefaultPolicy(role.isTokenNoDefaultPolicy())
                        .setTokenNumUses(role.getTokenNumUses())
                        .setTokenPeriod(toDurationSeconds(role.getTokenPeriod()))
                        .setTokenType(role.getTokenType() != null ? role.getTokenType().getValue() : null));
    }

    @Override
    public Uni<String> getAppRoleRoleId(String name) {
        return appRole.readRoleId(name);
    }

    @Override
    public Uni<Void> setAppRoleRoleId(String name, String roleId) {
        return appRole.updateRoleId(name, roleId);
    }

    @Override
    public Uni<VaultAppRoleSecretId> createNewSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        var params = new VaultAuthAppRoleGenerateSecretIdParams()
                .setCidrList(newSecretIdRequest.cidrList)
                .setTokenBoundCidrs(newSecretIdRequest.tokenBoundCidrs)
                .setMetadata(newSecretIdRequest.metadata);
        return appRole.generateSecretId(name, params)
                .map(result -> new VaultAppRoleSecretId()
                        .setSecretId(result.getSecretId())
                        .setSecretIdAccessor(result.getSecretIdAccessor()));
    }

    @Override
    public Uni<VaultAppRoleSecretId> createCustomSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        var params = new VaultAuthAppRoleCreateCustomSecretIdParams()
                .setSecretId(newSecretIdRequest.secretId)
                .setCidrList(newSecretIdRequest.cidrList)
                .setTokenBoundCidrs(newSecretIdRequest.tokenBoundCidrs)
                .setMetadata(newSecretIdRequest.metadata);
        return appRole.createCustomSecretId(name, params)
                .map(result -> new VaultAppRoleSecretId()
                        .setSecretId(result.getSecretId())
                        .setSecretIdAccessor(result.getSecretIdAccessor()));
    }

    @Override
    public Uni<List<String>> getSecretIdAccessors(String name) {
        return appRole.listSecretIdAccessors(name);
    }

    @Override
    public Uni<VaultAppRoleSecretIdAccessor> getSecretIdAccessor(String name, String accessorId) {
        return appRole.readSecretIdAccessor(name, accessorId)
                .map(result -> new VaultAppRoleSecretIdAccessor()
                        .setCreationTime(result.getCreationTime())
                        .setLastUpdatedTime(result.getLastUpdatedTime())
                        .setExpirationTime(result.getExpirationTime())
                        .setSecretIdAccessor(result.getSecretIdAccessor())
                        .setMetadata(result.getMetadata())
                        .setCidrList(result.getCidrList())
                        .setSecretIdNumUses(result.getSecretIdNumUses())
                        .setSecretIdTtl(toDurationSeconds(result.getSecretIdTtl()))
                        .setTokenBoundCidrs(result.getTokenBoundCidrs()));
    }

    @Override
    public Uni<Void> deleteSecretIdAccessor(String name, String accessorId) {
        return appRole.destroySecretIdAccessor(name, accessorId);
    }

    @Override
    public Uni<VaultAppRoleSecretIdAccessor> getSecretId(String name, String secretId) {
        return appRole.readSecretId(name, secretId)
                .map(result -> new VaultAppRoleSecretIdAccessor()
                        .setCreationTime(result.getCreationTime())
                        .setLastUpdatedTime(result.getLastUpdatedTime())
                        .setExpirationTime(result.getExpirationTime())
                        .setSecretIdAccessor(result.getSecretIdAccessor())
                        .setMetadata(result.getMetadata())
                        .setCidrList(result.getCidrList())
                        .setSecretIdNumUses(result.getSecretIdNumUses())
                        .setSecretIdTtl(toDurationSeconds(result.getSecretIdTtl()))
                        .setTokenBoundCidrs(result.getTokenBoundCidrs()));
    }

    @Override
    public Uni<Void> deleteSecretId(String name, String secretId) {
        return appRole.destroySecretId(name, secretId);
    }

}

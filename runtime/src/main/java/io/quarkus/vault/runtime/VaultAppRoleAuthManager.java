package io.quarkus.vault.runtime;

import java.util.Collections;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultAppRoleAuthReactiveService;
import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthCreateCustomSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthCreateSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdAccessorData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthReadSecretIdData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthRoleData;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuthRoleIdData;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultAppRoleAuthManager implements VaultAppRoleAuthReactiveService {

    @Inject
    private VaultClient vaultClient;
    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalAppRoleAuthMethod vaultInternalAppRoleAuthMethod;

    @Override
    public Uni<List<String>> getAppRoles() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalAppRoleAuthMethod.listAuthRoles(vaultClient, token)
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
    public Uni<Void> createOrUpdateAppRole(String name, VaultAppRoleAuthRole role) {
        VaultAppRoleAuthRoleData body = new VaultAppRoleAuthRoleData()
                .setBindSecretId(role.bindSecretId)
                .setSecretIdBoundCidrs(role.secretIdBoundCidrs)
                .setSecretIdNumUses(role.secretIdNumUses)
                .setSecretIdTtl(role.secretIdTtl)
                .setLocalSecretIds(role.localSecretIds)
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
            return vaultInternalAppRoleAuthMethod.createAuthRole(vaultClient, token, name, body);
        });
    }

    @Override
    public Uni<Void> deleteAppRole(String name) {
        return vaultAuthManager.getClientToken(vaultClient)
                .flatMap(token -> vaultInternalAppRoleAuthMethod.deleteAuthRoles(vaultClient, token, name));
    }

    @Override
    public Uni<VaultAppRoleAuthRole> getAppRole(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalAppRoleAuthMethod.getVaultAuthRole(vaultClient, token, name)
                    .map(result -> {
                        VaultAppRoleAuthRoleData role = result.data;
                        return new VaultAppRoleAuthRole()
                                .setBindSecretId(role.bindSecretId)
                                .setSecretIdBoundCidrs(role.secretIdBoundCidrs)
                                .setSecretIdNumUses(role.secretIdNumUses)
                                .setSecretIdTtl(role.secretIdTtl)
                                .setLocalSecretIds(role.localSecretIds)
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

    @Override
    public Uni<String> getAppRoleRoleId(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalAppRoleAuthMethod.getRoleId(vaultClient, token, name)
                    .map(result -> result.data.roleId);
        });
    }

    @Override
    public Uni<Void> setAppRoleRoleId(String name, String roleId) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            VaultAppRoleAuthRoleIdData body = new VaultAppRoleAuthRoleIdData()
                    .setRoleId(roleId);
            return vaultInternalAppRoleAuthMethod.setRoleId(vaultClient, token, name, body);
        });
    }

    @Override
    public Uni<VaultAppRoleSecretId> createNewSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            VaultAppRoleAuthCreateSecretIdData body = new VaultAppRoleAuthCreateSecretIdData()
                    .setCidrList(newSecretIdRequest.cidrList)
                    .setTokenBoundCidrs(newSecretIdRequest.tokenBoundCidrs)
                    .setMetadata(newSecretIdRequest.metadata);
            return vaultInternalAppRoleAuthMethod.createSecretId(vaultClient, token, name, body)
                    .map(result -> new VaultAppRoleSecretId()
                            .setSecretId(result.data.secretId)
                            .setSecretIdAccessor(result.data.secretIdAccessor));
        });
    }

    @Override
    public Uni<VaultAppRoleSecretId> createCustomSecretId(String name, VaultAppRoleSecretIdRequest newSecretIdRequest) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            VaultAppRoleAuthCreateCustomSecretIdData body = new VaultAppRoleAuthCreateCustomSecretIdData()
                    .setSecretId(newSecretIdRequest.secretId)
                    .setCidrList(newSecretIdRequest.cidrList)
                    .setTokenBoundCidrs(newSecretIdRequest.tokenBoundCidrs)
                    .setMetadata(newSecretIdRequest.metadata);
            return vaultInternalAppRoleAuthMethod.createCustomSecretId(vaultClient, token, name, body)
                    .map(result -> new VaultAppRoleSecretId()
                            .setSecretId(result.data.secretId)
                            .setSecretIdAccessor(result.data.secretIdAccessor));
        });
    }

    @Override
    public Uni<List<String>> getSecretIdAccessors(String name) {
        return vaultAuthManager.getClientToken(vaultClient)
                .flatMap(token -> vaultInternalAppRoleAuthMethod.listSecretIdAccessors(vaultClient, token, name)
                        .map(r -> r.data.keys)
                        .onFailure(VaultClientException.class).recoverWithUni(e -> {
                            if (((VaultClientException) e).getStatus() == 404) {
                                return Uni.createFrom().item(Collections.emptyList());
                            } else {
                                return Uni.createFrom().failure(e);
                            }
                        }));
    }

    @Override
    public Uni<VaultAppRoleSecretIdAccessor> getSecretIdAccessor(String name, String accessorId) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            VaultAppRoleAuthReadSecretIdAccessorData body = new VaultAppRoleAuthReadSecretIdAccessorData()
                    .setSecretIdAccessor(accessorId);
            return vaultInternalAppRoleAuthMethod.readSecretIdAccessor(vaultClient, token, name, body)
                    .map(result -> new VaultAppRoleSecretIdAccessor()
                            .setCreationTime(result.data.creationTime)
                            .setLastUpdatedTime(result.data.lastUpdatedTime)
                            .setExpirationTime(result.data.expirationTime)
                            .setSecretIdAccessor(result.data.secretIdAccessor)
                            .setMetadata(result.data.metadata)
                            .setCidrList(result.data.cidrList)
                            .setSecretIdNumUses(result.data.secretIdNumUses)
                            .setSecretIdTtl(result.data.secretIdTtl)
                            .setTokenBoundCidrs(result.data.tokenBoundCidrs));
        });
    }

    @Override
    public Uni<Void> deleteSecretIdAccessor(String name, String accessorId) {
        VaultAppRoleAuthReadSecretIdAccessorData body = new VaultAppRoleAuthReadSecretIdAccessorData()
                .setSecretIdAccessor(accessorId);
        return vaultAuthManager.getClientToken(vaultClient)
                .flatMap(token -> vaultInternalAppRoleAuthMethod.deleteSecretIdAccessor(vaultClient, token, name, body));
    }

    @Override
    public Uni<VaultAppRoleSecretIdAccessor> getSecretId(String name, String secretId) {
        return vaultAuthManager.getClientToken(vaultClient)
                .flatMap(token -> {

                    VaultAppRoleAuthReadSecretIdData body = new VaultAppRoleAuthReadSecretIdData()
                            .setSecretId(secretId);

                    return vaultInternalAppRoleAuthMethod.readSecretId(vaultClient, token, name, body)
                            .map(result -> new VaultAppRoleSecretIdAccessor()
                                    .setCreationTime(result.data.creationTime)
                                    .setLastUpdatedTime(result.data.lastUpdatedTime)
                                    .setExpirationTime(result.data.expirationTime)
                                    .setSecretIdAccessor(result.data.secretIdAccessor)
                                    .setMetadata(result.data.metadata)
                                    .setCidrList(result.data.cidrList)
                                    .setSecretIdNumUses(result.data.secretIdNumUses)
                                    .setSecretIdTtl(result.data.secretIdTtl)
                                    .setTokenBoundCidrs(result.data.tokenBoundCidrs));
                });
    }

    @Override
    public Uni<Void> deleteSecretId(String name, String secretId) {
        VaultAppRoleAuthReadSecretIdData body = new VaultAppRoleAuthReadSecretIdData()
                .setSecretId(secretId);
        return vaultAuthManager.getClientToken(vaultClient)
                .flatMap(token -> vaultInternalAppRoleAuthMethod.deleteSecretId(vaultClient, token, name, body));
    }

}

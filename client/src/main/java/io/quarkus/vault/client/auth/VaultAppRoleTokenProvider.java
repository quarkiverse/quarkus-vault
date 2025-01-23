package io.quarkus.vault.client.auth;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRole;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultAppRoleTokenProvider implements VaultTokenProvider {
    private final String mountPath;
    private final String roleId;
    private final Function<VaultAuthRequest, CompletionStage<String>> secretIdProvider;

    public VaultAppRoleTokenProvider(String mountPath, String roleId,
            Function<VaultAuthRequest, CompletionStage<String>> secretIdProvider) {
        this.mountPath = mountPath;
        this.roleId = roleId;
        this.secretIdProvider = secretIdProvider;
    }

    public VaultAppRoleTokenProvider(VaultAppRoleAuthOptions options) {
        this(options.mountPath, options.roleId, options.secretIdProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();
        return secretIdProvider.apply(authRequest)
                .thenCompose(secretId -> executor.execute(VaultAuthAppRole.FACTORY.login(mountPath, roleId, secretId)))
                .thenApply(VaultResponse::getResult)
                .thenApply(res -> {
                    var auth = res.getAuth();
                    return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                            auth.getNumUses(), authRequest.getInstantSource());
                });
    }
}

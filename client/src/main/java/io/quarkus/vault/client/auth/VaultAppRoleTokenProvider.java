package io.quarkus.vault.client.auth;

import java.util.function.Function;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRole;
import io.quarkus.vault.client.common.VaultResponse;
import io.smallrye.mutiny.Uni;

public class VaultAppRoleTokenProvider implements VaultTokenProvider {
    private final String mountPath;
    private final String roleId;
    private final Function<VaultAuthRequest, Uni<String>> secretIdProvider;

    public VaultAppRoleTokenProvider(String mountPath, String roleId,
            Function<VaultAuthRequest, Uni<String>> secretIdProvider) {
        this.mountPath = mountPath;
        this.roleId = roleId;
        this.secretIdProvider = secretIdProvider;
    }

    public VaultAppRoleTokenProvider(VaultAppRoleAuthOptions options) {
        this(options.mountPath, options.roleId, options.secretIdProvider);
    }

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();
        return secretIdProvider.apply(authRequest)
                .flatMap(secretId -> executor.execute(VaultAuthAppRole.FACTORY.login(mountPath, roleId, secretId)))
                .map(VaultResponse::getResult)
                .map(res -> {
                    var auth = res.getAuth();
                    return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                            authRequest.getInstantSource());
                });
    }
}

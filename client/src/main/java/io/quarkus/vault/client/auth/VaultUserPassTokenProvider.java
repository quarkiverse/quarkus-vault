package io.quarkus.vault.client.auth;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPass;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultUserPassTokenProvider implements VaultTokenProvider {
    private final String mountPath;
    private final String username;
    private final Function<VaultAuthRequest, CompletionStage<String>> passwordProvider;

    public VaultUserPassTokenProvider(String mountPath, String username,
            Function<VaultAuthRequest, CompletionStage<String>> passwordProvider) {
        this.mountPath = mountPath;
        this.username = username;
        this.passwordProvider = passwordProvider;
    }

    public VaultUserPassTokenProvider(VaultUserPassAuthOptions options) {
        this(options.mountPath, options.username, options.passwordProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();
        return passwordProvider.apply(authRequest).thenCompose(password -> {
            return executor.execute(VaultAuthUserPass.FACTORY.login(mountPath, username, password))
                    .thenApply(VaultResponse::getResult)
                    .thenApply(res -> {
                        var auth = res.getAuth();
                        return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                                auth.getNumUses(), authRequest.getInstantSource());
                    });
        });
    }
}

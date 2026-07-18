package io.quarkus.vault.client.auth;

import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.api.auth.github.VaultAuthGithub;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultGithubTokenProvider implements VaultTokenProvider {
    private final String mountPath;
    private final Function<VaultAuthRequest, CompletionStage<String>> tokenProvider;

    public VaultGithubTokenProvider(String mountPath,
            Function<VaultAuthRequest, CompletionStage<String>> tokenProvider) {
        this.mountPath = mountPath;
        this.tokenProvider = tokenProvider;
    }

    public VaultGithubTokenProvider(VaultGithubAuthOptions options) {
        this(options.mountPath, options.tokenProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();
        return tokenProvider.apply(authRequest).thenCompose(token -> {
            return executor.execute(VaultAuthGithub.FACTORY.login(mountPath, token))
                    .thenApply(VaultResponse::getResult)
                    .thenApply(res -> {
                        var auth = res.getAuth();
                        return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                                auth.getNumUses(), authRequest.getInstantSource());
                    });
        });
    }
}

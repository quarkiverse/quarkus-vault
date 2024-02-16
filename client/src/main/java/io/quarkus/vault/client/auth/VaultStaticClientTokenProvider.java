package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

public class VaultStaticClientTokenProvider implements VaultTokenProvider {
    private final Function<VaultAuthRequest, CompletionStage<String>> tokenProvider;

    public VaultStaticClientTokenProvider(Function<VaultAuthRequest, CompletionStage<String>> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public VaultStaticClientTokenProvider(VaultStaticClientTokenAuthOptions options) {
        this(options.tokenProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        return tokenProvider.apply(authRequest)
                .thenApply(token -> VaultToken.neverExpires(token, authRequest.getInstantSource()));
    }

    @Override
    public VaultTokenProvider caching(Duration renewGracePeriod) {
        // no caching for static token
        return this;
    }
}

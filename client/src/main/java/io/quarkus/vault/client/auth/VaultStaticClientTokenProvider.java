package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.util.function.Function;

import io.smallrye.mutiny.Uni;

public class VaultStaticClientTokenProvider implements VaultTokenProvider {
    private final Function<VaultAuthRequest, Uni<String>> tokenProvider;

    public VaultStaticClientTokenProvider(Function<VaultAuthRequest, Uni<String>> tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    public VaultStaticClientTokenProvider(VaultStaticClientTokenAuthOptions options) {
        this(options.tokenProvider);
    }

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
        return tokenProvider.apply(authRequest).map(VaultToken::neverExpires);
    }

    @Override
    public VaultTokenProvider caching(Duration renewGracePeriod) {
        // no caching for static token
        return this;
    }
}

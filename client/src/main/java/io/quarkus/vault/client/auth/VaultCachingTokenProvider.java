package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.util.OptionalCompletionStages.flatMapEmptyGet;
import static io.quarkus.vault.client.util.OptionalCompletionStages.flatMapPresent;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.auth.token.VaultAuthToken;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultCachingTokenProvider implements VaultTokenProvider {

    public static Duration DEFAULT_RENEW_GRACE_PERIOD = Duration.ofSeconds(30);

    private static final Logger log = Logger.getLogger(VaultCachingTokenProvider.class.getName());

    private final VaultTokenProvider delegate;
    private final Duration renewGracePeriod;
    private final AtomicReference<VaultToken> cachedToken = new AtomicReference<>(null);

    public VaultCachingTokenProvider(VaultTokenProvider delegate, Duration renewGracePeriod) {
        this.delegate = delegate;
        this.renewGracePeriod = renewGracePeriod;
    }

    public Optional<VaultToken> getCachedToken() {
        return Optional.ofNullable(cachedToken.get());
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {

        var cachedToken = getCachedToken()
                .map(token -> {
                    var logLevel = authRequest.getRequest().getLogConfidentialityLevel();
                    if (!token.hasAllowedUsesRemaining()) {
                        log.fine("cached token " + token.getConfidentialInfo(logLevel) + " has exhausted its allowed usages");
                        return null;
                    }
                    if (token.isExpired()) {
                        log.fine("cached token " + token.getConfidentialInfo(logLevel) + " has expired");
                        return null;
                    }
                    log.fine("using cached token " + token.getConfidentialInfo(logLevel) + " (expires at "
                            + token.getExpiresAt() + ")");
                    return token;
                });

        return CompletableFuture.completedStage(cachedToken)
                // if present, extend token if necessary
                .thenCompose(flatMapPresent(token -> {
                    if (token.shouldExtend(renewGracePeriod)) {
                        return extend(authRequest, token.getClientToken());
                    }
                    return CompletableFuture.completedStage(token);
                }))
                // if empty, request new token from delegate
                .thenCompose(flatMapEmptyGet(() -> request(authRequest)))
                // cache token
                .thenApply(vaultToken -> {
                    this.cachedToken.set(vaultToken.cached());
                    return vaultToken;
                });
    }

    @Override
    public void invalidateCache() {
        cachedToken.set(null);
    }

    @Override
    public VaultTokenProvider caching(Duration renewGracePeriod) {
        // no caching for caching token provider
        return this;
    }

    public CompletionStage<VaultToken> request(VaultAuthRequest authRequest) {
        var logLevel = authRequest.getRequest().getLogConfidentialityLevel();
        return delegate.apply(authRequest)
                .thenApply(vaultToken -> {
                    sanityCheck(vaultToken);
                    log.fine("created new login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                });
    }

    public CompletionStage<VaultToken> extend(VaultAuthRequest authRequest, String clientToken) {
        var logLevel = authRequest.getRequest().getLogConfidentialityLevel();
        var request = VaultAuthToken.FACTORY.renewSelf(null)
                .builder()
                .token(clientToken)
                .rebuild();
        return authRequest.getExecutor().execute(request)
                .thenApply(VaultResponse::getResult)
                .thenApply(res -> {
                    var auth = res.getAuth();
                    var vaultToken = VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                            auth.getNumUses(), authRequest.getInstantSource());
                    sanityCheck(vaultToken);
                    log.fine("extended login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                })
                .exceptionallyCompose(e -> {
                    if (e instanceof CompletionException) {
                        e = e.getCause();
                    }

                    if (e instanceof VaultClientException ve) {
                        if (ve.isPermissionDenied() || ve.hasErrorContaining("lease is not renewable")) {
                            // token is invalid
                            log.fine("login token " + clientToken + " has become invalid");
                            return CompletableFuture.completedStage(null);
                        }
                    }
                    return CompletableFuture.failedStage(e);
                });
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", renewGracePeriod);
    }

}

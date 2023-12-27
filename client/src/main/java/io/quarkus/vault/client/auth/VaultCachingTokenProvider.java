package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.util.OptionalUnis.flatMapEmptyGet;
import static io.quarkus.vault.client.util.OptionalUnis.flatMapPresent;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.auth.token.VaultAuthToken;
import io.quarkus.vault.client.common.VaultResponse;
import io.smallrye.mutiny.Uni;

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

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {

        var cachedToken = Optional.ofNullable(this.cachedToken.get())
                .map(token -> {
                    if (token.isExpired()) {
                        log.fine("cached token " + token.getClientToken() + " has expired");
                        return null;
                    }
                    log.fine("using cached token " + token.getClientToken() + " (expires at " + token.getExpiresAt() + ")");
                    return token;
                });

        return Uni.createFrom().item(cachedToken)
                // if present, extend token if necessary
                .plug(flatMapPresent(token -> {
                    if (token.shouldExtend(renewGracePeriod)) {
                        return extend(authRequest, token.getClientToken());
                    }
                    return Uni.createFrom().item(token);
                }))
                // if empty, request new token from delegate
                .plug(flatMapEmptyGet(() -> request(authRequest)))
                // cache token
                .map(vaultToken -> {
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

    public Uni<VaultToken> request(VaultAuthRequest authRequest) {
        var logLevel = authRequest.getRequest().getLogConfidentialityLevel();
        return delegate.apply(authRequest)
                .map(vaultToken -> {
                    sanityCheck(vaultToken);
                    log.fine("created new login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                });
    }

    public Uni<VaultToken> extend(VaultAuthRequest authRequest, String clientToken) {
        var logLevel = authRequest.getRequest().getLogConfidentialityLevel();
        var request = VaultAuthToken.FACTORY.renewSelf(null)
                .builder()
                .token(clientToken)
                .rebuild();
        return authRequest.getExecutor().execute(request)
                .map(VaultResponse::getResult)
                .map(res -> {
                    var auth = res.getAuth();
                    var vaultToken = VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                            authRequest.getInstantSource());
                    sanityCheck(vaultToken);
                    log.fine("extended login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                })
                .onFailure(VaultClientException.class).recoverWithUni(e -> {
                    var ve = (VaultClientException) e;
                    if (ve.isPermissionDenied() || ve.hasErrorContaining("lease is not renewable")) {
                        // token is invalid
                        log.fine("login token " + clientToken + " has become invalid");
                        return Uni.createFrom().nullItem();
                    }
                    return Uni.createFrom().failure(e);
                });
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", renewGracePeriod);
    }

}

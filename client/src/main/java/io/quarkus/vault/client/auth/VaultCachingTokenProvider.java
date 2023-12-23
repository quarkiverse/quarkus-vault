package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.auth.token.VaultAuthToken;
import io.quarkus.vault.client.api.auth.token.VaultAuthTokenLookupSelfResult;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.smallrye.mutiny.Uni;

public class VaultCachingTokenProvider implements VaultTokenProvider {

    public static Duration DEFAULT_RENEW_GRACE_PERIOD = Duration.ofSeconds(30);

    private static final Logger log = Logger.getLogger(VaultCachingTokenProvider.class.getName());

    private final VaultTokenProvider provider;
    private final Duration renewGracePeriod;
    private final AtomicReference<VaultToken> cachedToken = new AtomicReference<>(null);

    public VaultCachingTokenProvider(VaultTokenProvider provider, Duration renewGracePeriod) {
        this.provider = provider;
        this.renewGracePeriod = renewGracePeriod;
    }

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
        return Uni.createFrom().item(Optional.ofNullable(cachedToken.get()))
                // check clientToken is still valid
                .flatMap(vaultToken -> validate(authRequest, vaultToken))
                // extend clientToken if necessary
                .flatMap(vaultToken -> {
                    if (vaultToken.isPresent() && vaultToken.get().shouldExtend(renewGracePeriod)) {
                        return extend(authRequest, vaultToken.get().clientToken).map(Optional::of);
                    }
                    return Uni.createFrom().item(vaultToken);
                })
                // create new clientToken if necessary
                .flatMap(vaultToken -> {
                    if (vaultToken.isEmpty() || vaultToken.get().isExpired()
                            || vaultToken.get().expiresSoon(renewGracePeriod)) {
                        return login(authRequest);
                    }
                    return Uni.createFrom().item(vaultToken.get());
                })
                .map(vaultToken -> {
                    cachedToken.set(vaultToken);
                    return vaultToken;
                });
    }

    @Override
    public VaultTokenProvider caching(Duration renewGracePeriod) {
        // no caching for cached token
        return this;
    }

    private Uni<VaultToken> login(VaultAuthRequest authRequest) {
        var logLevel = authRequest.request().getLogConfidentialityLevel();
        return provider.apply(authRequest)
                .map(vaultToken -> {
                    sanityCheck(vaultToken);
                    log.fine("created new login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                });
    }

    private Uni<Optional<VaultToken>> validate(VaultAuthRequest authRequest, Optional<VaultToken> vaultToken) {
        if (vaultToken.isEmpty()) {
            return Uni.createFrom().item(Optional.empty());
        }
        VaultRequest<VaultAuthTokenLookupSelfResult> request = VaultAuthToken.FACTORY.lookupSelf()
                .builder()
                .token(vaultToken.get().clientToken)
                .rebuild();
        return authRequest.executor().execute(request)
                .map(i -> vaultToken)
                .onFailure(VaultClientException.class).recoverWithUni(e -> {
                    if (((VaultClientException) e).getStatus() == 403) { // forbidden
                        log.fine("login token " + vaultToken.get().clientToken + " has become invalid");
                        return Uni.createFrom().item(Optional.empty());
                    } else {
                        return Uni.createFrom().failure(e);
                    }
                });
    }

    private Uni<VaultToken> extend(VaultAuthRequest authRequest, String clientToken) {
        var logLevel = authRequest.request().getLogConfidentialityLevel();
        var request = VaultAuthToken.FACTORY.renewSelf(null)
                .builder()
                .token(clientToken)
                .rebuild();
        return authRequest.executor().execute(request)
                .map(VaultResponse::getResult)
                .map(res -> {
                    var auth = res.getAuth();
                    var vaultToken = new VaultToken(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration());
                    sanityCheck(vaultToken);
                    log.fine("extended login token: " + vaultToken.getConfidentialInfo(logLevel));
                    return vaultToken;
                });
    }

    private void sanityCheck(VaultToken vaultToken) {
        vaultToken.leaseDurationSanityCheck("auth", renewGracePeriod);
    }

}

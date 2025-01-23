package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;
import static java.lang.Math.max;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultToken extends VaultTimeLimited {

    private final String clientToken;
    private final boolean fromCache;
    private final AtomicInteger allowedUsesRemaining;

    public static VaultToken from(String clientToken, boolean renewable, Duration leaseDuration, Integer allowedUsesRemaining,
            InstantSource instantSource) {
        return new VaultToken(clientToken, renewable, leaseDuration, allowedUsesRemaining, false, instantSource);
    }

    public static VaultToken renewable(String clientToken, Duration leaseDuration, Integer allowedUsesRemaining,
            InstantSource instantSource) {
        return from(clientToken, true, leaseDuration, allowedUsesRemaining, instantSource);
    }

    public static VaultToken neverExpires(String clientToken, InstantSource instantSource) {
        return from(clientToken, false, Duration.ofSeconds(Long.MAX_VALUE), null, instantSource);
    }

    public VaultToken(String clientToken, boolean renewable, Duration leaseDuration, Integer allowedUsesRemaining,
            boolean fromCache,
            InstantSource instantSource) {
        super(renewable, leaseDuration, instantSource);
        this.clientToken = clientToken;
        this.fromCache = fromCache;
        this.allowedUsesRemaining = new AtomicInteger(
                allowedUsesRemaining != null && allowedUsesRemaining > 0 ? allowedUsesRemaining : Integer.MAX_VALUE);
    }

    public VaultToken(String clientToken, boolean renewable, Duration leaseDuration, Instant created, boolean fromCache,
            AtomicInteger allowedUsesRemaining, InstantSource instantSource) {
        super(renewable, leaseDuration, created, instantSource);
        this.clientToken = clientToken;
        this.fromCache = fromCache;
        this.allowedUsesRemaining = allowedUsesRemaining;
    }

    public VaultToken cached() {
        return new VaultToken(clientToken, isRenewable(), getLeaseDuration(), getCreated(), true, allowedUsesRemaining,
                getInstantSource());
    }

    public String getClientToken() {
        return clientToken;
    }

    public int getAllowedUsesRemaining() {
        return allowedUsesRemaining.get();
    }

    public boolean hasAllowedUsesRemaining() {
        return allowedUsesRemaining.get() > 0;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    /**
     * Returns the client token for usage, decrementing the allowed uses remaining.
     * <p>
     * During normal operation, this method should always return the client token, as the remaining
     * use count should be checked before calling this method. However, if the token is requested for use
     * multiple times in parallel, it is possible that the use count will be exhausted between the check and the
     * call to this method. In that case, the method will throw a {@link VaultTokenException}. The
     * {@link io.quarkus.vault.client.VaultClient} implementation should handle this exception and retry the
     * request, after obtaining a new token (if possible).
     *
     * @apiNote This ignores the token's expiration; it is the caller's responsibility to ensure the token has not expired.
     * @return the client token
     * @throws VaultTokenException if the token has no allowed uses remaining
     */
    public String getClientTokenForUsage() {
        var remainingUses = allowedUsesRemaining.updateAndGet(remaining -> max(remaining - 1, -1));
        if (remainingUses == -1) {
            throw new VaultTokenException(VaultTokenException.Reason.TOKEN_USES_EXHAUSTED);
        }
        return clientToken;
    }

    @Override
    public boolean isValid() {
        return super.isValid() && hasAllowedUsesRemaining();
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{clientToken: " + level.maskWithTolerance(clientToken, LOW) + ", " + super.info() + "}";
    }

}

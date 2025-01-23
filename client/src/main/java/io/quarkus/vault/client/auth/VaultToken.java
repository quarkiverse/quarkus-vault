package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.concurrent.atomic.AtomicInteger;

import io.quarkus.vault.client.VaultException;
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

    public boolean isFromCache() {
        return fromCache;
    }

    public String getClientTokenForUsage() {
        if (isNotUsable()) {
            throw new VaultException("Token has exhausted its allowed uses of " + allowedUsesRemaining.get());
        }
        allowedUsesRemaining.updateAndGet(remaining -> remaining > 0 ? remaining - 1 : 0);
        return clientToken;
    }

    public boolean isNotUsable() {
        return allowedUsesRemaining.get() <= 0;
    }

    @Override
    public boolean isExpired() {
        return isNotUsable() || super.isExpired();
    }

    @Override
    public boolean isExpiringWithin(Duration gracePeriod) {
        return isNotUsable() || super.isExpiringWithin(gracePeriod);
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{clientToken: " + level.maskWithTolerance(clientToken, LOW) + ", " + super.info() + "}";
    }

}

package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.logging.Logger;

public abstract class VaultTimeLimited {

    private static final Logger log = Logger.getLogger(VaultTimeLimited.class.getName());

    private final InstantSource instantSource;
    private final Instant created;
    private final boolean renewable;
    private final Duration leaseDuration;

    public VaultTimeLimited(boolean renewable, Duration leaseDuration, InstantSource instantSource) {
        this(renewable, leaseDuration, instantSource.instant(), instantSource);
    }

    public VaultTimeLimited(boolean renewable, Duration leaseDuration, Instant created, InstantSource instantSource) {
        this.instantSource = instantSource;
        this.created = created;
        this.renewable = renewable;
        this.leaseDuration = leaseDuration;
    }

    public InstantSource getInstantSource() {
        return instantSource;
    }

    public Instant getCreated() {
        return created;
    }

    public boolean isRenewable() {
        return renewable;
    }

    public Duration getLeaseDuration() {
        return leaseDuration;
    }

    /**
     * Check if the token is valid, in the sense that using it for authentication
     * *should* be successful.
     * <p>
     * This base class implementation checks if the token is expired, but subclasses
     * may override this method to provide additional checks.
     *
     * @return true if the token is valid, false otherwise
     */
    public boolean isValid() {
        return !isExpired();
    }

    public boolean isExpired() {
        return instantSource.instant().isAfter(getExpiresAt());
    }

    public boolean isExpiringWithin(Duration gracePeriod) {
        return instantSource.instant().plus(gracePeriod).isAfter(getExpiresAt());
    }

    public boolean shouldExtend(Duration gracePeriod) {
        return !isExpired() && renewable && isExpiringWithin(gracePeriod);
    }

    public Instant getExpiresAt() {
        return created.plus(leaseDuration);
    }

    public String info() {
        return "renewable: " + renewable + ", leaseDuration: " + leaseDuration + ", valid_until: " + getExpiresAt();
    }

    public void leaseDurationSanityCheck(String nickname, Duration gracePeriod) {
        if (leaseDuration.compareTo(gracePeriod) < 0) {
            log.warning(nickname + " lease duration " + leaseDuration
                    + "s is smaller than the renew grace period "
                    + gracePeriod.getSeconds() + "s");
        }
    }

}

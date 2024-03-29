package io.quarkus.vault.client.auth;

import java.time.Duration;
import java.time.Instant;
import java.time.InstantSource;
import java.util.logging.Logger;

public abstract class VaultTimeLimited {

    private static final Logger log = Logger.getLogger(VaultTimeLimited.class.getName());

    InstantSource instantSource;
    Instant created;
    public boolean renewable;
    public Duration leaseDuration;

    public VaultTimeLimited(boolean renewable, Duration leaseDuration, InstantSource instantSource) {
        this.instantSource = instantSource;
        this.created = instantSource.instant();
        this.renewable = renewable;
        this.leaseDuration = leaseDuration;
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

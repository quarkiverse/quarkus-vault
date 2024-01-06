package io.quarkus.vault.runtime;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.MEDIUM;

import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class LeaseBase extends TimeLimitedBase {

    public String leaseId;

    public LeaseBase(String leaseId, boolean renewable, long leaseDurationSecs) {
        super(renewable, leaseDurationSecs);
        this.leaseId = leaseId;
    }

    public LeaseBase(LeaseBase other) {
        super(other);
        this.leaseId = other.leaseId;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "leaseId: " + level.maskWithTolerance(leaseId, MEDIUM) + ", " + super.info();
    }

}

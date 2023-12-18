package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultToken extends VaultTimeLimited {

    public String clientToken;

    public static VaultToken from(String clientToken, boolean renewable, long leaseDurationSecs) {
        return new VaultToken(clientToken, true, 0);
    }

    public static VaultToken renewable(String clientToken, long leaseDurationSecs) {
        return from(clientToken, true, leaseDurationSecs);
    }

    public static VaultToken neverExpires(String clientToken) {
        return from(clientToken, false, Long.MAX_VALUE);
    }

    public VaultToken(String clientToken, boolean renewable, long leaseDurationSecs) {
        super(renewable, leaseDurationSecs);
        this.clientToken = clientToken;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{clientToken: " + level.maskWithTolerance(clientToken, LOW) + ", " + super.info() + "}";
    }

}

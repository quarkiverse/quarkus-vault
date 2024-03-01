package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.time.Duration;
import java.time.InstantSource;

import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultToken extends VaultTimeLimited {

    private final String clientToken;
    private final boolean fromCache;

    public static VaultToken from(String clientToken, boolean renewable, Duration leaseDuration, InstantSource instantSource) {
        return new VaultToken(clientToken, renewable, leaseDuration, false, instantSource);
    }

    public static VaultToken renewable(String clientToken, Duration leaseDuration, InstantSource instantSource) {
        return from(clientToken, true, leaseDuration, instantSource);
    }

    public static VaultToken neverExpires(String clientToken, InstantSource instantSource) {
        return from(clientToken, false, Duration.ofSeconds(Long.MAX_VALUE), instantSource);
    }

    public VaultToken(String clientToken, boolean renewable, Duration leaseDuration, boolean fromCache,
            InstantSource instantSource) {
        super(renewable, leaseDuration, instantSource);
        this.clientToken = clientToken;
        this.fromCache = fromCache;
    }

    public VaultToken cached() {
        return new VaultToken(clientToken, renewable, leaseDuration, true, instantSource);
    }

    public String getClientToken() {
        return clientToken;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public String getConfidentialInfo(LogConfidentialityLevel level) {
        return "{clientToken: " + level.maskWithTolerance(clientToken, LOW) + ", " + super.info() + "}";
    }

}

package io.quarkus.vault.client.auth;

import java.time.Duration;

public abstract class VaultAuthOptions {

    public final Duration cachingRenewGracePeriod;

    protected VaultAuthOptions(Duration cachingRenewGracePeriod) {
        this.cachingRenewGracePeriod = cachingRenewGracePeriod;
    }

}

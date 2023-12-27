package io.quarkus.vault.client.auth;

import java.time.InstantSource;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAuthRequest {

    private final VaultRequestExecutor executor;
    private final VaultRequest<?> request;
    private final InstantSource instantSource;

    public VaultAuthRequest(VaultRequestExecutor executor, VaultRequest<?> request, InstantSource instantSource) {
        this.executor = executor;
        this.request = request;
        this.instantSource = instantSource;
    }

    public static VaultAuthRequest of(VaultRequestExecutor executor, VaultRequest<?> request, InstantSource instantSource) {
        return new VaultAuthRequest(executor, request, instantSource);
    }

    public VaultRequestExecutor getExecutor() {
        return executor;
    }

    public VaultRequest<?> getRequest() {
        return request;
    }

    public InstantSource getInstantSource() {
        return instantSource;
    }
}

package io.quarkus.vault.client.auth;

import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAuthRequest {

    private final VaultRequestExecutor executor;
    private final VaultRequest<?> request;

    public VaultAuthRequest(VaultRequestExecutor executor, VaultRequest<?> request) {
        this.executor = executor;
        this.request = request;
    }

    public static VaultAuthRequest of(VaultRequestExecutor executor, VaultRequest<?> request) {
        return new VaultAuthRequest(executor, request);
    }

    public VaultRequestExecutor executor() {
        return executor;
    }

    public VaultRequest<?> request() {
        return request;
    }

}

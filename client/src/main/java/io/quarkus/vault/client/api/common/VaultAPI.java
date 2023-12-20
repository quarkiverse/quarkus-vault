package io.quarkus.vault.client.api.common;

import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAPI {

    protected final VaultRequestExecutor executor;

    public VaultAPI(VaultRequestExecutor executor) {
        this.executor = executor;
    }

    public VaultRequestExecutor getExecutor() {
        return executor;
    }

}

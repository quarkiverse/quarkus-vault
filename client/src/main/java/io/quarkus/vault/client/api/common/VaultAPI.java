package io.quarkus.vault.client.api.common;

import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAPI {

    protected final VaultRequestExecutor executor;
    protected final String mountPath;

    public VaultAPI(VaultRequestExecutor executor, String mountPath) {
        this.executor = executor;
        this.mountPath = mountPath;
    }

    public VaultRequestExecutor getExecutor() {
        return executor;
    }

    public String getMountPath() {
        return mountPath;
    }

}

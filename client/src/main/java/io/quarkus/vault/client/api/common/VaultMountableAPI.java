package io.quarkus.vault.client.api.common;

import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultMountableAPI<F extends VaultRequestFactory> extends VaultAPI<F> {

    protected final String mountPath;

    public VaultMountableAPI(VaultRequestExecutor executor, F factory, String mountPath) {
        super(executor, factory);
        this.mountPath = mountPath;
    }

    public String getMountPath() {
        return mountPath;
    }

}

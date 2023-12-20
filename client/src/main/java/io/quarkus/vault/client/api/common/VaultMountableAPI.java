package io.quarkus.vault.client.api.common;

import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultMountableAPI extends VaultAPI {

    protected final String mountPath;

    public VaultMountableAPI(VaultRequestExecutor executor, String mountPath) {
        super(executor);
        this.mountPath = mountPath;
    }

    public String getMountPath() {
        return mountPath;
    }

}

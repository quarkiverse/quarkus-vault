package io.quarkus.vault.client.api;

import io.quarkus.vault.client.api.sys.health.VaultSysHealth;
import io.quarkus.vault.client.api.sys.init.VaultSysInit;
import io.quarkus.vault.client.api.sys.seal.VaultSysSeal;
import io.quarkus.vault.client.api.sys.wrapping.VaultSysWrapping;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultSysAPIAccessor {

    private final VaultRequestExecutor executor;

    public VaultSysAPIAccessor(VaultRequestExecutor executor) {
        this.executor = executor;
    }

    public VaultSysHealth health() {
        return new VaultSysHealth(executor, null);
    }

    public VaultSysInit init() {
        return new VaultSysInit(executor, null);
    }

    public VaultSysSeal seal() {
        return new VaultSysSeal(executor, null);
    }

    public VaultSysWrapping wrapping() {
        return new VaultSysWrapping(executor, null);
    }

}

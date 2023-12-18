package io.quarkus.vault.client.api;

import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultSecretsAPIAccessor {

    private final VaultRequestExecutor executor;

    public VaultSecretsAPIAccessor(VaultRequestExecutor executor) {
        this.executor = executor;
    }

    public VaultSecretsKV1 kv1() {
        return kv1("secret");
    }

    public VaultSecretsKV1 kv1(String mountPath) {
        return new VaultSecretsKV1(executor, mountPath);
    }

    public VaultSecretsKV2 kv2() {
        return kv2("secret");
    }

    public VaultSecretsKV2 kv2(String mountPath) {
        return new VaultSecretsKV2(executor, mountPath);
    }

}

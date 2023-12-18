package io.quarkus.vault.client.api;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRole;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetes;
import io.quarkus.vault.client.api.auth.token.VaultAuthToken;
import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPass;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAuthAPIAccessor {

    public static final String TOKEN_MOUNT_PATH = "token";
    public static final String DEFAULT_KUBERNETES_MOUNT_PATH = "kubernetes";
    public static final String DEFAULT_APPROLE_MOUNT_PATH = "approle";
    public static final String DEFAULT_USERPASS_MOUNT_PATH = "userpass";

    private final VaultRequestExecutor executor;

    public VaultAuthAPIAccessor(VaultRequestExecutor executor) {
        this.executor = executor;
    }

    public VaultAuthToken token() {
        return new VaultAuthToken(executor, TOKEN_MOUNT_PATH);
    }

    public VaultAuthKubernetes kubernetes() {
        return kubernetes(DEFAULT_KUBERNETES_MOUNT_PATH);
    }

    public VaultAuthKubernetes kubernetes(String mountPath) {
        return new VaultAuthKubernetes(executor, mountPath);
    }

    public VaultAuthAppRole appRole() {
        return appRole(DEFAULT_APPROLE_MOUNT_PATH);
    }

    public VaultAuthAppRole appRole(String mountPath) {
        return new VaultAuthAppRole(executor, mountPath);
    }

    public VaultAuthUserPass userPass() {
        return userPass(DEFAULT_USERPASS_MOUNT_PATH);
    }

    public VaultAuthUserPass userPass(String mountPath) {
        return new VaultAuthUserPass(executor, mountPath);
    }

}

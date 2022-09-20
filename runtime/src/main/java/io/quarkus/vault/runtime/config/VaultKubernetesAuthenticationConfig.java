package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_KUBERNETES_AUTH_MOUNT_PATH;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_KUBERNETES_JWT_TOKEN_PATH;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultKubernetesAuthenticationConfig {

    /**
     * Kubernetes authentication role that has been created in Vault to associate Vault policies, with
     * Kubernetes service accounts and/or Kubernetes namespaces. This property is required when selecting
     * the Kubernetes authentication type.
     */
    Optional<String> role();

    /**
     * Location of the file containing the Kubernetes JWT token to authenticate against
     * in Kubernetes authentication mode.
     */
    @WithDefault(DEFAULT_KUBERNETES_JWT_TOKEN_PATH)
    String jwtTokenPath();

    /**
     * Allows configure Kubernetes authentication mount path.
     */
    @WithDefault(DEFAULT_KUBERNETES_AUTH_MOUNT_PATH)
    String authMountPath();
}

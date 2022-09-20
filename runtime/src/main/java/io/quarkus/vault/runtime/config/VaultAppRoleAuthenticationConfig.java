package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_APPROLE_AUTH_MOUNT_PATH;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultAppRoleAuthenticationConfig {

    /**
     * Role Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    Optional<String> roleId();

    /**
     * Secret Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    Optional<String> secretId();

    /**
     * Wrapping token containing a Secret Id, obtained from:
     * <p>
     * vault write -wrap-ttl=60s -f auth/approle/role/myapp/secret-id
     * <p>
     * secret-id and secret-id-wrapping-token are exclusive
     */
    Optional<String> secretIdWrappingToken();

    /**
     * Allows configure Approle authentication mount path.
     */
    @WithDefault(DEFAULT_APPROLE_AUTH_MOUNT_PATH)
    String authMountPath();
}

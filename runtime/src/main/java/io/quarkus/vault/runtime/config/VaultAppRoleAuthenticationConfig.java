package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultBootstrapConfig.DEFAULT_APPROLE_AUTH_MOUNT_PATH;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultAppRoleAuthenticationConfig {

    /**
     * Role Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    @ConfigItem
    public Optional<String> roleId;

    /**
     * Secret Id for AppRole auth method. This property is required when selecting the app-role authentication type.
     */
    @ConfigItem
    public Optional<String> secretId;

    /**
     * Wrapping token containing a Secret Id, obtained from:
     * <p>
     * vault write -wrap-ttl=60s -f auth/approle/role/myapp/secret-id
     * <p>
     * secret-id and secret-id-wrapping-token are exclusive
     */
    @ConfigItem
    public Optional<String> secretIdWrappingToken;

    /**
     * Allows configure Approle authentication mount path.
     */
    @ConfigItem(defaultValue = DEFAULT_APPROLE_AUTH_MOUNT_PATH)
    public String authMountPath;
}

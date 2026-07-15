package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_GITHUB_AUTH_MOUNT_PATH;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultGithubAuthenticationConfig {

    /**
     * GitHub personal access token for the github auth method. This property is required when selecting
     * the github authentication type.
     */
    Optional<String> token();

    /**
     * Wrapping token containing a GitHub token, obtained from:
     * <p>
     * vault kv get -wrap-ttl=60s secret/<path>
     * <p>
     * The key has to be 'token', meaning the GitHub token has initially been provisioned with:
     * <p>
     * vault kv put secret/<path> token=<github token value>
     * <p>
     * token and token-wrapping-token are exclusive
     */
    Optional<String> tokenWrappingToken();

    /**
     * Allows configure github authentication mount path.
     */
    @WithDefault(DEFAULT_GITHUB_AUTH_MOUNT_PATH)
    String authMountPath();
}

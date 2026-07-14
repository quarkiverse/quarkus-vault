package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultAuthenticationConfig {

    /**
     * Disable authentication: no client token will be sent to Vault. This is useful when the Vault url points to
     * a Vault Agent with Auto-Auth enabled, which authenticates on behalf of the application and injects the Vault
     * token into proxied requests. This property is exclusive with all other authentication settings.
     * <p>
     * See <a href="https://developer.hashicorp.com/vault/docs/agent-and-proxy/autoauth">Vault Agent Auto-Auth</a>
     */
    @WithDefault("false")
    boolean none();

    /**
     * Vault token, bypassing Vault authentication (kubernetes, userpass or approle). This is useful in development
     * where an authentication mode might not have been set up. In production we will usually prefer some
     * authentication such as userpass, or preferably kubernetes, where Vault tokens get generated with a TTL
     * and some ability to revoke them. Lease renewal does not apply.
     */
    Optional<String> clientToken();

    /**
     * Client token wrapped in a wrapping token, such as what is returned by:
     * <p>
     * vault token create -wrap-ttl=60s -policy=myapp
     * <p>
     * client-token and client-token-wrapping-token are exclusive. Lease renewal does not apply.
     */
    Optional<String> clientTokenWrappingToken();

    /**
     * AppRole authentication method
     * <p>
     * See <a href="https://www.vaultproject.io/api/auth/approle/index.html">AppRole Auth Method</a>
     */
    VaultAppRoleAuthenticationConfig appRole();

    /**
     * Userpass authentication method
     * <p>
     * See <a href="https://www.vaultproject.io/api/auth/userpass/index.html">Userpass Auth Role</a>
     */
    VaultUserpassAuthenticationConfig userpass();

    /**
     * Kubernetes authentication method
     * <p>
     * See <a href="https://www.vaultproject.io/docs/auth/kubernetes.html">Kubernetes Auth Method</a>
     */
    VaultKubernetesAuthenticationConfig kubernetes();

    /**
     * GitHub authentication method
     * <p>
     * See <a href="https://developer.hashicorp.com/vault/api-docs/auth/github">GitHub Auth Method</a>
     */
    VaultGithubAuthenticationConfig github();

    /**
     * AWS IAM authentication method
     * <p>
     * See <a href="https://developer.hashicorp.com/vault/api-docs/auth/aws">AWS Auth Method</a>
     */
    VaultAwsIamAuthenticationConfig awsIam();

    default boolean isDirectClientToken() {
        return clientToken().isPresent() || clientTokenWrappingToken().isPresent();
    }

    default boolean isAppRole() {
        return appRole().roleId().isPresent()
                && (appRole().secretId().isPresent() || appRole().secretIdWrappingToken().isPresent());
    }

    default boolean isUserpass() {
        return userpass().username().isPresent()
                && (userpass().password().isPresent() || userpass().passwordWrappingToken().isPresent());
    }

    default boolean isGithub() {
        return github().token().isPresent() || github().tokenWrappingToken().isPresent();
    }

    default boolean isAwsIam() {
        return awsIam().role().isPresent();
    }
}

package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface VaultUserpassAuthenticationConfig {

    /**
     * User for userpass auth method. This property is required when selecting the userpass authentication type.
     */
    Optional<String> username();

    /**
     * Password for userpass auth method. This property is required when selecting the userpass authentication type.
     */
    Optional<String> password();

    /**
     * Wrapping token containing a Password, obtained from:
     * <p>
     * vault kv get -wrap-ttl=60s secret/<path>
     * <p>
     * The key has to be 'password', meaning the password has initially been provisioned with:
     * <p>
     * vault kv put secret/<path> password=<password value>
     * <p>
     * password and password-wrapping-token are exclusive
     */
    Optional<String> passwordWrappingToken();
}

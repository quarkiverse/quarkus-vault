package io.quarkus.vault.runtime.config;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface CredentialsProviderConfig {
    String DATABASE_DEFAULT_MOUNT = "database";
    String RABBITMQ_DEFAULT_MOUNT = "rabbitmq";
    String DEFAULT_REQUEST_PATH = "creds";

    /**
     * Database credentials role, as defined by
     * <a href="https://www.vaultproject.io/docs/secrets/databases/index.html">Vault Databases</a>
     * <p>
     * Only one of `database-credentials-role`, `credentials-role` or `kv-path` can be defined.
     *
     * @deprecated Use `credentials-role` with `credentials-mount` set to `database`
     * @asciidoclet
     */
    @Deprecated(since = "2.6")
    Optional<String> databaseCredentialsRole();

    /**
     * Dynamic credentials' role.
     * <p>
     * Roles are defined by the secret engine in use. For example, `database` credentials roles are defined
     * by the database secrets engine described at
     * <a href="https://www.vaultproject.io/docs/secrets/databases/index.html">Vault Databases</a>.
     * <p>
     * One of `credentials-role` or `kv-path` can to be defined. not both.
     *
     * @asciidoclet
     */
    Optional<String> credentialsRole();

    /**
     * Mount of dynamic credentials secrets engine, for example `database` or `rabbitmq`.
     * <p>
     * Only used when `credentials-role` is defined.
     *
     * @asciidoclet
     */
    @WithDefault(DATABASE_DEFAULT_MOUNT)
    String credentialsMount();

    /**
     * Path of dynamic credentials request.
     * <p>
     * Request paths are dictated by the secret engine in use. For standard secret engines this should be
     * left as the default of `creds`.
     * <p>
     * Only used when `credentials-role` is defined.
     *
     * @asciidoclet
     */
    @WithDefault(DEFAULT_REQUEST_PATH)
    String credentialsRequestPath();

    /**
     * A path in vault kv store, where we will find the kv-key.
     * <p>
     * One of `database-credentials-role` or `kv-path` needs to be defined. not both.
     * <p>
     * see <a href="https://www.vaultproject.io/docs/secrets/kv/index.html">KV Secrets Engine</a>
     *
     * @asciidoclet
     */
    Optional<String> kvPath();

    /**
     * Key name to search in vault path `kv-path`. The value for that key is the credential.
     * <p>
     * `kv-key` should not be defined if `kv-path` is not.
     * <p>
     * see <a href="https://www.vaultproject.io/docs/secrets/kv/index.html">KV Secrets Engine</a>
     *
     * @asciidoclet
     */
    @WithDefault(PASSWORD_PROPERTY_NAME)
    String kvKey();

    @Override
    String toString();
}

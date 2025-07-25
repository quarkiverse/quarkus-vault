package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.client.logging.LogConfidentialityLevel.MEDIUM;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;

import java.net.URL;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigDocMapKey;
import io.quarkus.runtime.annotations.ConfigDocSection;
import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.quarkus.runtime.configuration.DurationConverter;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithConverter;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

@ConfigMapping(prefix = "quarkus.vault")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface VaultRuntimeConfig {
    String NAME = "vault";
    String DEFAULT_CONFIG_ORDINAL = "270";
    String DEFAULT_KUBERNETES_JWT_TOKEN_PATH = "/var/run/secrets/kubernetes.io/serviceaccount/token";
    String DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH = "secret";
    String DEFAULT_TRANSIT_SECRET_ENGINE_MOUNT_PATH = "transit";
    String KV_SECRET_ENGINE_VERSION_V2 = "2";
    String DEFAULT_RENEW_GRACE_PERIOD = "1H";
    String DEFAULT_SECRET_CONFIG_CACHE_PERIOD = "10M";
    String KUBERNETES_CACERT = "/var/run/secrets/kubernetes.io/serviceaccount/ca.crt";
    String DEFAULT_CONNECT_TIMEOUT = "5S";
    String DEFAULT_READ_TIMEOUT = "5S";
    String DEFAULT_TLS_USE_KUBERNETES_CACERT = "true";
    String DEFAULT_KUBERNETES_AUTH_MOUNT_PATH = "kubernetes";
    String DEFAULT_APPROLE_AUTH_MOUNT_PATH = "approle";
    String DEFAULT_USERPASS_AUTH_MOUNT_PATH = "userpass";

    @WithName("kv-secret-engine")
    @ConfigDocMapKey("alias")
    Map<String, KvSecretEngineConfig> kvSecretEngineAlias();

    /**
     * Microprofile Config ordinal.
     * <p>
     * This is provided as an alternative to the `config_ordinal` property defined by the specification, to
     * make it easier and more natural for applications to override the default ordinal.
     * <p>
     * The default value is higher than the file system or jar ordinals, but lower than env vars.
     */
    @WithDefault(DEFAULT_CONFIG_ORDINAL)
    int configOrdinal();

    /**
     * Vault server url.
     * <p>
     * Example: https://localhost:8200
     * <p>
     * See also the documentation for the `kv-secret-engine-mount-path` property for some insights on how
     * the full Vault url gets built.
     *
     * @asciidoclet
     */
    Optional<URL> url();

    /**
     * Vault Enterprise
     */
    @ConfigDocSection
    VaultEnterpriseConfig enterprise();

    /**
     * Authentication
     */
    @ConfigDocSection
    VaultAuthenticationConfig authentication();

    /**
     * Renew grace period duration.
     * <p>
     * This value if used to extend a lease before it expires its ttl, or recreate a new lease before the current
     * lease reaches its max_ttl.
     * By default Vault leaseDuration is equal to 7 days (ie: 168h or 604800s).
     * If a connection pool maxLifetime is set, it is reasonable to set the renewGracePeriod to be greater
     * than the maxLifetime, so that we are sure we get a chance to renew leases before we reach the ttl.
     * In any case you need to make sure there will be attempts to fetch secrets within the renewGracePeriod,
     * because that is when the renewals will happen. This is particularly important for db dynamic secrets
     * because if the lease reaches its ttl or max_ttl, the password of the db user will become invalid and
     * it will be not longer possible to log in.
     * This value should also be smaller than the ttl, otherwise that would mean that we would try to recreate
     * leases all the time.
     *
     * @asciidoclet
     */
    @WithDefault(DEFAULT_RENEW_GRACE_PERIOD)
    @WithConverter(DurationConverter.class)
    Duration renewGracePeriod();

    /**
     * Vault config source cache period.
     * <p>
     * Properties fetched from vault as MP config will be kept in a cache, and will not be fetched from vault
     * again until the expiration of that period.
     * This property is ignored if `secret-config-kv-path` is not set.
     *
     * @asciidoclet
     */
    @WithDefault(DEFAULT_SECRET_CONFIG_CACHE_PERIOD)
    @WithConverter(DurationConverter.class)
    Duration secretConfigCachePeriod();

    // @formatter:off
    /**
     * List of comma separated vault paths in kv store,
     * where all properties will be available as MP config properties **as-is**, with no prefix.
     * <p>
     * For instance, if vault contains property `foo`, it will be made available to the
     * quarkus application as `@ConfigProperty(name = "foo") String foo;`
     * <p>
     * If 2 paths contain the same property, the last path will win.
     * <p>
     * For instance if
     * <p>
     * * `secret/base-config` contains `foo=bar` and
     * * `secret/myapp/config` contains `foo=myappbar`, then
     * <p>
     * `@ConfigProperty(name = "foo") String foo` will have value `myappbar`
     * with application properties `quarkus.vault.secret-config-kv-path=base-config,myapp/config`
     * <p>
     * See also the documentation for the `kv-secret-engine-mount-path` property for some insights on how
     * the full Vault url gets built.
     *
     * @asciidoclet
     */
    // @formatter:on
    Optional<List<String>> secretConfigKvPath();

    /**
     * KV store paths configuration.
     */
    @WithName("secret-config-kv-path")
    @ConfigDocMapKey("prefix")
    Map<String, KvPathConfig> secretConfigKvPathPrefix();

    /**
     * Maximum number of attempts when fetching MP Config properties on the initial connection.
     */
    @WithDefault("1")
    int mpConfigInitialAttempts();

    /**
     * Used to hide confidential infos, for logging in particular.
     * Possible values are:
     * <p>
     * * low: display all secrets.
     * * medium: display only usernames and lease ids (ie: passwords and tokens are masked).
     * * high: hide lease ids and dynamic credentials username.
     *
     * @asciidoclet
     */
    @WithDefault("medium")
    LogConfidentialityLevel logConfidentialityLevel();

    /**
     * Kv secret engine version.
     * <p>
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @WithDefault(KV_SECRET_ENGINE_VERSION_V2)
    int kvSecretEngineVersion();

    /**
     * KV secret engine path.
     * <p>
     * This value is used when building the url path in the KV secret engine programmatic access
     * (i.e. `VaultKVSecretEngine`) and the vault config source (i.e. fetching configuration properties from Vault).
     * <p>
     * For a v2 KV secret engine (default - see `kv-secret-engine-version property`)
     * the full url is built from the expression `<url>/v1/</kv-secret-engine-mount-path>/data/...`.
     * <p>
     * With property `quarkus.vault.url=https://localhost:8200`, the following call
     * `vaultKVSecretEngine.readSecret("foo/bar")` would lead eventually to a `GET` on Vault with the following
     * url: `https://localhost:8200/v1/secret/data/foo/bar`.
     * <p>
     * With a KV secret engine v1, the url changes to: `<url>/v1/</kv-secret-engine-mount-path>/...`.
     * <p>
     * The same logic is applied to the Vault config source. With `quarkus.vault.secret-config-kv-path=config/myapp`
     * The secret properties would be fetched from Vault using a `GET` on
     * `https://localhost:8200/v1/secret/data/config/myapp` for a KV secret engine v2 (or
     * `https://localhost:8200/v1/secret/config/myapp` for a KV secret engine v1).
     * <p>
     * see https://www.vaultproject.io/docs/secrets/kv/index.html
     *
     * @asciidoclet
     */
    @WithDefault(DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH)
    String kvSecretEngineMountPath();

    /**
     * Transit secret engine mount path.
     * <p>
     * see https://www.vaultproject.io/docs/secrets/transit/index.html
     *
     * @asciidoclet
     */
    @WithDefault(DEFAULT_TRANSIT_SECRET_ENGINE_MOUNT_PATH)
    String transitSecretEngineMountPath();

    /**
     * TLS
     */
    @ConfigDocSection
    VaultTlsConfig tls();

    /**
     * Timeout to establish a connection with Vault.
     */
    @WithDefault(DEFAULT_CONNECT_TIMEOUT)
    @WithConverter(DurationConverter.class)
    Duration connectTimeout();

    /**
     * Request timeout on Vault.
     */
    @WithDefault(DEFAULT_READ_TIMEOUT)
    @WithConverter(DurationConverter.class)
    Duration readTimeout();

    /**
     * List of remote hosts that are not proxied when the client is configured to use a proxy. This
     * list serves the same purpose as the JVM {@code nonProxyHosts} configuration.
     *
     * <p>
     * Entries can use the <i>*</i> wildcard character for pattern matching, e.g <i>*.example.com</i> matches
     * <i>www.example.com</i>.
     */
    Optional<List<String>> nonProxyHosts();

    /**
     * The proxy host. If set the client is configured to use a proxy.
     */
    Optional<String> proxyHost();

    /**
     * The port the proxy is listening on, 3128 by default.
     */
    @WithDefault("3128")
    Integer proxyPort();

    /**
     * List of named credentials providers, such as: `quarkus.vault.credentials-provider.foo.kv-path=mypath`
     * <p>
     * This defines a credentials provider `foo` returning key `password` from vault path `mypath`.
     * Once defined, this provider can be used in credentials consumers, such as the Agroal connection pool.
     * <p>
     * Example: `quarkus.datasource.credentials-provider=foo`
     *
     * @asciidoclet
     */
    Map<String, CredentialsProviderConfig> credentialsProvider();

    /**
     * Transit Engine
     */
    @ConfigDocSection
    VaultTransitConfig transit();

    /**
     * Deprecated.
     */
    @Deprecated
    @WithName("devservices")
    Map<String, String> devServices();

    /**
     * Deprecated.
     */
    @Deprecated
    Map<String, String> health();

    default VaultAuthenticationType getAuthenticationType() {
        if (authentication().kubernetes().role().isPresent()) {
            return KUBERNETES;
        } else if (authentication().isUserpass()) {
            return USERPASS;
        } else if (authentication().isAppRole()) {
            return APPROLE;
        } else {
            return null;
        }
    }

    default String toStringConfidential() {
        return "VaultRuntimeConfig{" +
                "url=" + url() +
                ", kubernetesAuthenticationMountPath=" + authentication().kubernetes().authMountPath() +
                ", kubernetesAuthenticationRole="
                + logConfidentialityLevel().maskWithTolerance(authentication().kubernetes().role().orElse(""), MEDIUM) +
                ", kubernetesJwtTokenPath='" + authentication().kubernetes().jwtTokenPath() + '\'' +
                ", userpassUsername='"
                + logConfidentialityLevel().maskWithTolerance(authentication().userpass().username().orElse(""), MEDIUM) + '\''
                +
                ", userpassPassword='"
                + logConfidentialityLevel().maskWithTolerance(authentication().userpass().password().orElse(""), LOW) + '\'' +
                ", appRoleRoleId='"
                + logConfidentialityLevel().maskWithTolerance(authentication().appRole().roleId().orElse(""), MEDIUM) + '\'' +
                ", appRoleSecretId='"
                + logConfidentialityLevel().maskWithTolerance(authentication().appRole().secretId().orElse(""), LOW) + '\'' +
                ", appRoleSecretIdWrappingToken='"
                + logConfidentialityLevel().maskWithTolerance(authentication().appRole().secretIdWrappingToken().orElse(""),
                        LOW)
                + '\'' +
                ", clientToken=" + logConfidentialityLevel().maskWithTolerance(authentication().clientToken().orElse(""), LOW) +
                ", clientTokenWrappingToken="
                + logConfidentialityLevel().maskWithTolerance(authentication().clientTokenWrappingToken().orElse(""), LOW) +
                ", renewGracePeriod=" + renewGracePeriod() +
                ", cachePeriod=" + secretConfigCachePeriod() +
                ", logConfidentialityLevel=" + logConfidentialityLevel() +
                ", kvSecretEngineVersion=" + kvSecretEngineVersion() +
                ", kvSecretEngineMountPath='" + kvSecretEngineMountPath() + '\'' +
                ", additional named engines='" + kvSecretEngineAlias().size() + '\'' +
                ", tlsSkipVerify=" + tls().skipVerify() +
                ", tlsCaCert=" + tls().caCert() +
                ", connectTimeout=" + connectTimeout() +
                ", readTimeout=" + readTimeout() +
                '}';
    }

    @ConfigGroup
    interface KvPathConfig {
        // @formatter:off
        /**
         * List of comma separated vault paths in kv store,
         * where all properties will be available as **prefixed** MP config properties.
         * <p>
         * For instance if the application properties contains
         * `quarkus.vault.secret-config-kv-path.myprefix=config`, and
         * vault path `secret/config` contains `foo=bar`, then `myprefix.foo`
         * will be available in the MP config.
         * <p>
         * If the same property is available in 2 different paths for the same prefix, the last one
         * will win.
         * <p>
         * See also the documentation for the `quarkus.vault.kv-secret-engine-mount-path` property for some insights on how
         * the full Vault url gets built.
         *
         * @asciidoclet
         */
        // @formatter:on
        @WithParentName
        List<String> paths();

        @Override
        String toString();
    }

    @ConfigGroup
    interface KvSecretEngineConfig {

        /**
         * mount path for the named kv secret engine.
         */
        @WithDefault(DEFAULT_KV_SECRET_ENGINE_MOUNT_PATH)
        String mountPath();

        /**
         * paths to mount as MP config properties.
         */
        Optional<List<String>> secretConfigKvPath();

        /**
         * paths to mount as MP config properties. all properties will be prefixed with the key.
         */
        @WithName("secret-config-kv-path")
        @ConfigDocMapKey("prefix")
        Map<String, KvPathConfig> secretConfigKvPathPrefix();

        /**
         * version for the named kv secret engine.
         */
        @WithDefault(KV_SECRET_ENGINE_VERSION_V2)
        int version();
    }
}

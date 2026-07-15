package io.quarkus.it.vault.renew;

import java.util.HashMap;
import java.util.Map;

import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.images.builder.Transferable;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Starts a real Vault (dev mode) and a real PostgreSQL, connected on a shared docker network, and
 * configures:
 * <ul>
 * <li>the Vault database secrets engine with a dynamic credentials role for the postgres instance
 * (credentials lease TTL 10m, so much longer than the login token TTL),</li>
 * <li>a Vault auth method (approle or userpass, chosen with the {@code auth} init arg) issuing
 * login tokens with a TTL of {@link #TOKEN_TTL_SECONDS} (90s),</li>
 * <li>the Quarkus datasource with {@code quarkus.datasource.credentials-provider} pointing at the
 * Vault dynamic credentials role, and {@code quarkus.vault.renew-grace-period=60s}.</li>
 * </ul>
 * The database role uses the same revocation statements as the Vault postgres plugin defaults,
 * minus the {@code DROP ROLE}, so that lease revocation deterministically succeeds (and strips the
 * role of its privileges) even while the role still has open connections.
 */
public class VaultPostgresTestResource implements QuarkusTestResourceLifecycleManager {

    /** TTL of the Vault login tokens issued by the approle / userpass auth methods. */
    public static final int TOKEN_TTL_SECONDS = 90;

    /** Value of quarkus.vault.renew-grace-period. */
    public static final String RENEW_GRACE_PERIOD = "60s";

    private static final String POLICY = """
            path "database/creds/reproducer" {
              capabilities = ["read"]
            }
            path "secret/data/foo" {
              capabilities = ["read"]
            }
            path "sys/leases/renew" {
              capabilities = ["update"]
            }
            path "sys/leases/lookup" {
              capabilities = ["update"]
            }
            """;

    private String auth;
    private Network network;
    private PostgreSQLContainer<?> postgres;
    private VaultContainer<?> vault;

    @Override
    public void init(Map<String, String> initArgs) {
        auth = initArgs.getOrDefault("auth", "approle");
    }

    @Override
    public Map<String, String> start() {
        network = Network.newNetwork();

        postgres = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withNetwork(network)
                .withNetworkAliases("postgres")
                .withUsername("postgres")
                .withPassword("postgres")
                .withDatabaseName("postgres");
        postgres.start();
        exec(postgres, "psql", "-U", "postgres", "-d", "postgres", "-c",
                "CREATE TABLE demo(name TEXT); INSERT INTO demo VALUES ('ok');");

        vault = new VaultContainer<>(DockerImageName.parse("hashicorp/vault:1.15.2"))
                .withVaultToken("root")
                .withNetwork(network);
        vault.start();

        exec(vault, "vault", "secrets", "enable", "database");
        exec(vault, "vault", "write", "database/config/reproducer",
                "plugin_name=postgresql-database-plugin",
                "allowed_roles=reproducer",
                "connection_url=postgresql://{{username}}:{{password}}@postgres:5432/postgres?sslmode=disable",
                "username=postgres",
                "password=postgres");
        exec(vault, "vault", "write", "database/roles/reproducer",
                "db_name=reproducer",
                "creation_statements=CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}'"
                        + " VALID UNTIL '{{expiration}}';"
                        + " GRANT SELECT ON ALL TABLES IN SCHEMA public TO \"{{name}}\";",
                "revocation_statements=REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA public FROM \"{{name}}\";"
                        + " ALTER ROLE \"{{name}}\" WITH NOLOGIN;",
                "default_ttl=10m",
                "max_ttl=1h");

        vault.copyFileToContainer(Transferable.of(POLICY), "/tmp/reproducer-policy.hcl");
        exec(vault, "vault", "policy", "write", "reproducer", "/tmp/reproducer-policy.hcl");
        exec(vault, "vault", "kv", "put", "secret/foo", "greeting=hello");

        Map<String, String> config = new HashMap<>();
        if ("approle".equals(auth)) {
            exec(vault, "vault", "auth", "enable", "approle");
            exec(vault, "vault", "write", "auth/approle/role/reproducer",
                    "token_policies=reproducer",
                    "token_ttl=" + TOKEN_TTL_SECONDS,
                    "token_max_ttl=0");
            String roleId = exec(vault, "vault", "read", "-field=role_id", "auth/approle/role/reproducer/role-id");
            String secretId = exec(vault, "vault", "write", "-field=secret_id", "-f",
                    "auth/approle/role/reproducer/secret-id");
            config.put("quarkus.vault.authentication.app-role.role-id", roleId);
            config.put("quarkus.vault.authentication.app-role.secret-id", secretId);
        } else {
            exec(vault, "vault", "auth", "enable", "userpass");
            exec(vault, "vault", "write", "auth/userpass/users/bob",
                    "password=sinclair",
                    "token_policies=reproducer",
                    "token_ttl=" + TOKEN_TTL_SECONDS);
            config.put("quarkus.vault.authentication.userpass.username", "bob");
            config.put("quarkus.vault.authentication.userpass.password", "sinclair");
        }

        config.put("quarkus.vault.url", "http://" + vault.getHost() + ":" + vault.getMappedPort(8200));
        config.put("quarkus.vault.renew-grace-period", RENEW_GRACE_PERIOD);
        config.put("quarkus.vault.credentials-provider.reproducer.database-credentials-role", "reproducer");
        config.put("quarkus.datasource.jdbc.url", postgres.getJdbcUrl());
        return config;
    }

    @Override
    public void stop() {
        if (vault != null) {
            vault.stop();
        }
        if (postgres != null) {
            postgres.stop();
        }
        if (network != null) {
            network.close();
        }
    }

    private static String exec(GenericContainer<?> container, String... command) {
        try {
            Container.ExecResult result = container.execInContainer(command);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException(
                        "command " + String.join(" ", command) + " failed: " + result.getStderr());
            }
            return result.getStdout().trim();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

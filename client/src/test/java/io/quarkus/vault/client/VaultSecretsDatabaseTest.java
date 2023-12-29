package io.quarkus.vault.client;

import static io.quarkus.vault.client.api.secrets.database.VaultSecretsDatabaseConfigParamsBuilders.postgresBuilder;
import static io.quarkus.vault.client.api.secrets.database.VaultSecretsDatabasePostgresPasswordAuthentication.SCRAM_SHA_256;
import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.vault.client.api.secrets.database.*;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest(secrets = {
        @VaultClientTest.Mount(path = "database", type = "database"),
})
@Testcontainers
public class VaultSecretsDatabaseTest {

    @Container
    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:14")
            .withNetwork(Network.SHARED)
            .withNetworkAliases("pg")
            .withDatabaseName("test")
            .withUsername("test")
            .withPassword("test");

    @Test
    public void testConfigConnection(VaultClient client, @Random String connName) {
        var dbApi = client.secrets().database();

        var roleName = connName + "-role";

        var connUrl = "postgresql://{{username}}:{{password}}@pg:5432/test";

        dbApi.configureConnection(connName, postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl(connUrl)
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        var config = dbApi.readConnection(connName)
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.getPluginName())
                .isEqualTo("postgresql-database-plugin");
        assertThat(config.getPluginVersion())
                .isEmpty();
        assertThat(config.getAllowedRoles())
                .containsExactly(roleName);
        assertThat(config.getConnectionDetails())
                .containsEntry("connection_url", connUrl)
                .containsEntry("username", postgres.getUsername())
                .containsEntry("password_authentication", "scram-sha-256");

    }

    @Test
    public void testUpdateRole(VaultClient client, @Random String roleName) {
        var dbApi = client.secrets().database();

        dbApi.updateRole(roleName, new VaultSecretsDatabaseUpdateRoleParams()
                .setDbName("test")
                .setDefaultTtl(Duration.ofSeconds(10))
                .setMaxTtl(Duration.ofSeconds(20))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD)
                .setCredentialConfig(new VaultSecretsDatabasePasswordCredentialConfig()
                        .setPasswordPolicy("test")))
                .await().indefinitely();

        var role = dbApi.readRole(roleName)
                .await().indefinitely();

        assertThat(role)
                .isNotNull();
        assertThat(role.getDbName())
                .isEqualTo("test");
        assertThat(role.getDefaultTtl())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(role.getMaxTtl())
                .isEqualTo(Duration.ofSeconds(20));
        assertThat(role.getCredentialType())
                .isEqualTo(VaultSecretsDatabaseCredentialType.PASSWORD);
        assertThat(role.getCredentialConfig())
                .isInstanceOf(VaultSecretsDatabasePasswordCredentialConfig.class);
        assertThat(((VaultSecretsDatabasePasswordCredentialConfig) role.getCredentialConfig()).getPasswordPolicy())
                .isEqualTo("test");
        assertThat(role.getCreationStatements())
                .isEmpty();
        assertThat(role.getRevocationStatements())
                .isEmpty();
        assertThat(role.getRollbackStatements())
                .isEmpty();
        assertThat(role.getRenewStatements())
                .isEmpty();
    }

    @Test
    public void testListRoles(VaultClient client, @Random String roleName) {
        var dbApi = client.secrets().database();

        dbApi.updateRole(roleName, new VaultSecretsDatabaseUpdateRoleParams()
                .setDbName("test")
                .setDefaultTtl(Duration.ofSeconds(10))
                .setMaxTtl(Duration.ofSeconds(20))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD)
                .setCredentialConfig(new VaultSecretsDatabasePasswordCredentialConfig()
                        .setPasswordPolicy("test")))
                .await().indefinitely();

        var roles = dbApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(roleName);
    }

    @Test
    public void testDeleteRole(VaultClient client, @Random String roleName) {
        var dbApi = client.secrets().database();

        dbApi.updateRole(roleName, new VaultSecretsDatabaseUpdateRoleParams()
                .setDbName("test")
                .setDefaultTtl(Duration.ofSeconds(10))
                .setMaxTtl(Duration.ofSeconds(20))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD)
                .setCredentialConfig(new VaultSecretsDatabasePasswordCredentialConfig()
                        .setPasswordPolicy("test")))
                .await().indefinitely();

        var roles = dbApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(roleName);

        dbApi.deleteRole(roleName)
                .await().indefinitely();

        roles = dbApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .doesNotContain(roleName);
    }

    @Test
    public void testGenerateCredentials(VaultClient client, @Random String connName) throws Exception {
        var dbApi = client.secrets().database();

        var connUrl = "postgresql://{{username}}:{{password}}@pg:5432/test";
        var creationStatements = List.of(
                "CREATE ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}' VALID UNTIL '{{expiration}}' INHERIT;",
                "GRANT test TO \"{{name}}\";");
        var roleName = connName + "-role";

        dbApi.configureConnection(connName, postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl(connUrl)
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        dbApi.updateRole(roleName, new VaultSecretsDatabaseUpdateRoleParams()
                .setDbName(connName)
                .setDefaultTtl(Duration.ofSeconds(10))
                .setMaxTtl(Duration.ofSeconds(20))
                .setCreationStatements(creationStatements))
                .await().indefinitely();

        var credsResult = dbApi.generateCredentials(roleName)
                .await().indefinitely();

        assertThat(credsResult)
                .isNotNull();
        assertThat(credsResult.getLeaseDuration())
                .isEqualTo(Duration.ofSeconds(10));
        assertThat(credsResult.getData())
                .isNotNull();

        var data = credsResult.getData();

        var username = Objects.toString(data.get("username"));
        assertThat(username)
                .startsWith("v-token-");

        var password = Objects.toString(data.get("password"));
        assertThat(password)
                .isNotEmpty();

        testConnection(username, password);
    }

    @Test
    public void testRotateRootCredentials(VaultClient client, @Random String connName) throws Exception {

        var dbApi = client.secrets().database();

        createDbRole(connName);

        var roleName = connName + "-role";

        dbApi.configureConnection(connName, postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(connName)
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        dbApi.rotateRootCredentials(connName)
                .await().indefinitely();
    }

    @Test
    public void testUpdateStaticRole(VaultClient client, @Random String roleName) throws Exception {

        var dbApi = client.secrets().database();

        dbApi.configureConnection("test", postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        createDbRole(roleName);

        dbApi.updateStaticRole(roleName, new VaultSecretsDatabaseUpdateStaticRoleParams()
                .setDbName("test")
                .setUsername(roleName)
                .setRotationPeriod(Duration.ofDays(1))
                .setRotationStatements(List.of(
                        "ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';"))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD))
                .await().indefinitely();

        var role = dbApi.readStaticRole(roleName)
                .await().indefinitely();

        assertThat(role)
                .isNotNull();
        assertThat(role.getDbName())
                .isEqualTo("test");
        assertThat(role.getCredentialType())
                .isEqualTo(VaultSecretsDatabaseCredentialType.PASSWORD);
        assertThat(role.getRotationPeriod())
                .isEqualTo(Duration.ofDays(1));
        assertThat(role.getRotationStatements())
                .containsExactly("ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';");
    }

    @Test
    public void testListStaticRoles(VaultClient client, @Random String roleName) throws Exception {

        var dbApi = client.secrets().database();

        dbApi.configureConnection("test", postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        createDbRole(roleName);

        dbApi.updateStaticRole(roleName, new VaultSecretsDatabaseUpdateStaticRoleParams()
                .setDbName("test")
                .setUsername(roleName)
                .setRotationPeriod(Duration.ofDays(1))
                .setRotationStatements(List.of(
                        "ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';"))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD))
                .await().indefinitely();

        var roles = dbApi.listStaticRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(roleName);
    }

    @Test
    public void testDeleteStaticRole(VaultClient client, @Random String roleName) throws Exception {

        var dbApi = client.secrets().database();

        dbApi.configureConnection("test", postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        createDbRole(roleName);

        dbApi.updateStaticRole(roleName, new VaultSecretsDatabaseUpdateStaticRoleParams()
                .setDbName("test")
                .setUsername(roleName)
                .setRotationPeriod(Duration.ofDays(1))
                .setRotationStatements(List.of(
                        "ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';"))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD))
                .await().indefinitely();

        var roles = dbApi.listStaticRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(roleName);

        dbApi.deleteStaticRole(roleName)
                .await().indefinitely();

        roles = dbApi.listStaticRoles()
                .await().indefinitely();

        assertThat(roles)
                .doesNotContain(roleName);
    }

    @Test
    public void testGenerateStaticRoleCredentials(VaultClient client, @Random String roleName) throws Exception {

        var dbApi = client.secrets().database();

        dbApi.configureConnection("test", postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        createDbRole(roleName);

        dbApi.updateStaticRole(roleName, new VaultSecretsDatabaseUpdateStaticRoleParams()
                .setDbName("test")
                .setUsername(roleName)
                .setRotationPeriod(Duration.ofDays(1))
                .setRotationStatements(List.of(
                        "ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';"))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD))
                .await().indefinitely();

        var credsResult = dbApi.generateStaticRoleCredentials(roleName)
                .await().indefinitely();

        assertThat(credsResult)
                .isNotNull();
        assertThat(credsResult.getLeaseDuration())
                .isEqualTo(Duration.ZERO);
        assertThat(credsResult.getData())
                .isNotNull();

        var data = credsResult.getData();

        var username = Objects.toString(data.get("username"));
        assertThat(username)
                .isEqualTo(roleName);

        var password = Objects.toString(data.get("password"));
        assertThat(password)
                .isNotEmpty();

        testConnection(username, password);
    }

    @Test
    public void testRotateStaticRoleCredentials(VaultClient client, @Random String roleName) throws Exception {

        var dbApi = client.secrets().database();

        dbApi.configureConnection("test", postgresBuilder()
                .allowedRoles(List.of(roleName))
                .connectionUrl("postgresql://{{username}}:{{password}}@pg:5432/test")
                .username(postgres.getUsername())
                .password(postgres.getPassword())
                .passwordAuthentication(SCRAM_SHA_256)
                .build())
                .await().indefinitely();

        createDbRole(roleName);

        dbApi.updateStaticRole(roleName, new VaultSecretsDatabaseUpdateStaticRoleParams()
                .setDbName("test")
                .setUsername(roleName)
                .setRotationPeriod(Duration.ofDays(1))
                .setRotationStatements(List.of(
                        "ALTER ROLE \"{{name}}\" WITH LOGIN PASSWORD '{{password}}';"))
                .setCredentialType(VaultSecretsDatabaseCredentialType.PASSWORD))
                .await().indefinitely();

        var credsResult = dbApi.generateStaticRoleCredentials(roleName)
                .await().indefinitely();

        assertThat(credsResult)
                .isNotNull();
        assertThat(credsResult.getLeaseDuration())
                .isEqualTo(Duration.ZERO);
        assertThat(credsResult.getData())
                .isNotNull();

        var originalUsername = Objects.toString(credsResult.getData().get("username"));
        var originalPassword = Objects.toString(credsResult.getData().get("password"));

        dbApi.rotateStaticCredentials(roleName)
                .await().indefinitely();

        credsResult = dbApi.generateStaticRoleCredentials(roleName)
                .await().indefinitely();

        assertThat(credsResult)
                .isNotNull();
        assertThat(credsResult.getLeaseDuration())
                .isEqualTo(Duration.ZERO);
        assertThat(credsResult.getData())
                .isNotNull();

        var data = credsResult.getData();

        var username = Objects.toString(data.get("username"));
        assertThat(username)
                .isEqualTo(roleName)
                .isEqualTo(originalUsername);

        var password = Objects.toString(data.get("password"));
        assertThat(password)
                .isNotEmpty()
                .isNotEqualTo(originalPassword);

        testConnection(username, password);
    }

    private static void createDbRole(String username) throws SQLException {
        try (var conn = DriverManager.getConnection(postgres.getJdbcUrl(), "test", "test");
                var stmt = conn.createStatement()) {
            stmt.execute("CREATE ROLE \"" + username + "\" WITH LOGIN PASSWORD 'test' VALID UNTIL 'infinity' INHERIT;");
            stmt.execute("GRANT test TO \"" + username + "\";");
        }
    }

    private static void testConnection(String username, String password) throws SQLException {
        var jdbcUrl = "jdbc:postgresql://" + postgres.getHost() + ":" + postgres.getMappedPort(5432) + "/test";
        try (var conn = DriverManager.getConnection(jdbcUrl, username, password); var stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
        }
    }
}

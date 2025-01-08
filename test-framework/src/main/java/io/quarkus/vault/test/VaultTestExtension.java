package io.quarkus.vault.test;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static java.lang.Boolean.TRUE;
import static java.lang.String.format;
import static java.util.regex.Pattern.MULTILINE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.testcontainers.containers.BindMode.READ_ONLY;
import static org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.utility.DockerImageName;

import io.quarkus.vault.VaultKVSecretEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.api.sys.init.VaultSysInitParams;
import io.quarkus.vault.client.http.vertx.VertxVaultHttpClient;
import io.quarkus.vault.runtime.VaultVersions;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class VaultTestExtension {

    private static final Logger log = Logger.getLogger(VaultTestExtension.class.getName());

    static final String DB_NAME = "mydb";
    static final String DB_USERNAME = "postgres";
    public static final String DB_PASSWORD = "bar";
    static final String RMQ_USERNAME = "guest";
    public static final String RMQ_PASSWORD = "yXvOzyOPE";
    public static final String SECRET_VALUE = "s\u20accr\u20act";
    static final int VAULT_PORT = 8200;
    static final int MAPPED_POSTGRESQL_PORT = 6543;
    public static final String VAULT_AUTH_USERPASS_USER = "bob";
    public static final String VAULT_AUTH_USERPASS_PASSWORD = "sinclair";
    public static final String VAULT_AUTH_APPROLE = "myapprole";
    public static final String SECRET_PATH_V1 = "secret-v1";
    public static final String SECRET_PATH_V2 = "secret";
    public static final String LIST_PATH = "hello";
    public static final String LIST_SUB_PATH = "world";
    public static final String EXPECTED_SUB_PATHS = "[" + LIST_SUB_PATH + "]";
    public static final String VAULT_DBROLE = "mydbrole";
    public static final String VAULT_RMQROLE = "myrabbitmqrole";
    public static final String APP_SECRET_PATH = "foo";
    static final String APP_CONFIG_PATH = "config";
    static final String VAULT_POLICY = "mypolicy";
    static final String POSTGRESQL_HOST = "mypostgresdb";
    static final String RABBITMQ_HOST = "myrabbitmq";
    static final String VAULT_URL = (useTls() ? "https" : "http") + "://localhost:" + VAULT_PORT;
    public static final String SECRET_KEY = "secret";
    public static final String ENCRYPTION_KEY_NAME = "my-encryption-key";
    public static final String ENCRYPTION_KEY2_NAME = "my-encryption-key2";
    public static final String ENCRYPTION_DERIVED_KEY_NAME = "my-derivation-encryption-key";
    public static final String SIGN_KEY_NAME = "my-sign-key";
    public static final String SIGN_KEY2_NAME = "my-sign-key2";
    public static final String SIGN_DERIVATION_KEY_NAME = "my-derivation-sign-key";

    public static final String TMP_VAULT_POSTGRES_CREATION_SQL_FILE = "/tmp/vault-postgres-creation.sql";
    public static final String TMP_VAULT_CONFIG_JSON_FILE = "/tmp/vault-config.json";
    public static final String TMP_POSTGRES_INIT_SQL_FILE = "/tmp/postgres-init.sql";
    public static final String TEST_QUERY_STRING = "SELECT 1";
    public static final String CONTAINER_TMP_CMD = "/tmp/cmd";
    public static final String HOST_VAULT_TMP_CMD = "target/vault_cmd";
    public static final String HOST_POSTGRES_TMP_CMD = "target/postgres_cmd";
    public static final String OUT_FILE = "/out";
    public static final String WRAPPING_TEST_PATH = "wrapping-test";

    public static final String TEST_POSTGRES_CONFIG = "vault-test.postgres.version";
    public static final String TEST_RABBITMQ_CONFIG = "vault-test.rabbitmq.version";
    public static final String TEST_RABBITMQ_4_CONFIG = "vault-test.test-rabbitmq-4";
    public static final String TEST_POSTGRES_VERSION = PostgreSQLContainer.DEFAULT_TAG;
    public static final String TEST_RABBITMQ_VERSION_3 = "3.13.7-management-alpine";
    public static final String TEST_RABBITMQ_VERSION_4 = "4.0.2-management-alpine";

    private static final String CRUD_PATH = "crud";

    public GenericContainer vaultContainer;
    public PostgreSQLContainer postgresContainer;
    public RabbitMQContainer rabbitMQContainer;
    public String rootToken = null;
    public String appRoleSecretId = null;
    public String appRoleRoleId = null;
    public String appRoleSecretIdWrappingToken = null;
    public String clientTokenWrappingToken = null;
    public String passwordKvv1WrappingToken = null;
    public String passwordKvv2WrappingToken = null;
    public String anotherPasswordKvv2WrappingToken = null;

    private String db_default_ttl = "1m";
    private String db_max_ttl = "10m";

    private Config config = ConfigProvider.getConfig();

    public static void testDataSource(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (Statement stmt = c.createStatement()) {
                try (ResultSet rs = stmt.executeQuery(TEST_QUERY_STRING)) {
                    assertTrue(rs.next());
                    assertEquals(1, rs.getInt(1));
                }
            }
        }
    }

    public static void assertCrudSecret(VaultKVSecretEngine kvSecretEngine) {

        assertEquals(EXPECTED_SUB_PATHS, kvSecretEngine.listSecrets(LIST_PATH).toString());

        assertDeleteSecret(kvSecretEngine);

        assertDeleteSecret(kvSecretEngine);

        Map<String, String> newsecrets = new HashMap<>();
        newsecrets.put("first", "one");
        newsecrets.put("second", "two");
        kvSecretEngine.writeSecret(CRUD_PATH, newsecrets);
        assertEquals("{first=one, second=two}", readSecretAsString(kvSecretEngine, CRUD_PATH));

        newsecrets.put("first", "un");
        newsecrets.put("third", "tres");
        kvSecretEngine.writeSecret(CRUD_PATH, newsecrets);
        assertEquals("{first=un, second=two, third=tres}", readSecretAsString(kvSecretEngine, CRUD_PATH));

        assertDeleteSecret(kvSecretEngine);
    }

    private static void assertDeleteSecret(VaultKVSecretEngine kvSecretEngine) {
        kvSecretEngine.deleteSecret(CRUD_PATH);
        try {
            readSecretAsString(kvSecretEngine, CRUD_PATH);
        } catch (VaultClientException e) {
            assertEquals(404, e.getStatus());
        }
    }

    private static String readSecretAsString(VaultKVSecretEngine kvSecretEngine, String path) {
        Map<String, String> secret = kvSecretEngine.readSecret(path);
        return new TreeMap<>(secret).toString();
    }

    private VaultClient createVaultClient() {
        var webClient = WebClient.create(Vertx.vertx(), new WebClientOptions()
                .setTrustAll(true)
                .setVerifyHost(false));
        VertxVaultHttpClient httpClient = new VertxVaultHttpClient(webClient);
        return VaultClient.builder()
                .executor(httpClient)
                .baseUrl(getVaultUrl().orElseThrow())
                .build();
    }

    private static Optional<URL> getVaultUrl() {
        try {
            return Optional.of(new URL(VAULT_URL));
        } catch (MalformedURLException e) {
            throw new VaultException(e);
        }
    }

    public void start() throws Exception {

        log.info("start containers on " + System.getProperty("os.name"));

        new File(HOST_POSTGRES_TMP_CMD).mkdirs();

        Network network = Network.newNetwork();

        postgresContainer = new PostgreSQLContainer<>(DockerImageName.parse(PostgreSQLContainer.IMAGE).withTag(
                config.getOptionalValue(TEST_POSTGRES_CONFIG, String.class).orElse(TEST_POSTGRES_VERSION)))
                .withDatabaseName(DB_NAME)
                .withUsername(DB_USERNAME)
                .withPassword(DB_PASSWORD)
                .withNetwork(network)
                .withFileSystemBind(HOST_POSTGRES_TMP_CMD, CONTAINER_TMP_CMD)
                .withNetworkAliases(POSTGRESQL_HOST)
                .withExposedPorts(POSTGRESQL_PORT)
                .withClasspathResourceMapping("postgres-init.sql", TMP_POSTGRES_INIT_SQL_FILE, READ_ONLY);

        postgresContainer.setPortBindings(Arrays.asList(MAPPED_POSTGRESQL_PORT + ":" + POSTGRESQL_PORT));

        rabbitMQContainer = new RabbitMQContainer(
                ((DockerImageName) FieldUtils.readStaticField(RabbitMQContainer.class, "DEFAULT_IMAGE_NAME", true)).withTag(
                        config.getOptionalValue(TEST_RABBITMQ_CONFIG, String.class)
                                .orElse(config.getOptionalValue(TEST_RABBITMQ_4_CONFIG, Boolean.class).orElse(Boolean.FALSE)
                                        ? TEST_RABBITMQ_VERSION_4
                                        : TEST_RABBITMQ_VERSION_3)))
                .withAdminPassword(RMQ_PASSWORD)
                .withNetwork(network)
                .withNetworkAliases(RABBITMQ_HOST);

        String configFile = useTls() ? "vault-config-tls.json" : "vault-config.json";

        String vaultImage = getVaultImage();
        log.info("starting " + vaultImage + " with url=" + VAULT_URL + " and config file=" + configFile);

        new File(HOST_VAULT_TMP_CMD).mkdirs();

        vaultContainer = new GenericContainer<>(vaultImage)
                .withExposedPorts(VAULT_PORT)
                .withEnv("SKIP_SETCAP", "true")
                .withEnv("VAULT_SKIP_VERIFY", "true") // this is internal to the container
                .withEnv("VAULT_ADDR", VAULT_URL)
                .withNetwork(network)
                .withFileSystemBind(HOST_VAULT_TMP_CMD, CONTAINER_TMP_CMD)
                .withClasspathResourceMapping(configFile, TMP_VAULT_CONFIG_JSON_FILE, READ_ONLY)
                .withClasspathResourceMapping("vault-tls.key", "/tmp/vault-tls.key", READ_ONLY)
                .withClasspathResourceMapping("vault-tls.crt", "/tmp/vault-tls.crt", READ_ONLY)
                .withClasspathResourceMapping("vault-postgres-creation.sql", TMP_VAULT_POSTGRES_CREATION_SQL_FILE, READ_ONLY)
                .withClasspathResourceMapping("secret.json", "/tmp/secret.json", READ_ONLY)
                .withClasspathResourceMapping("config.json", "/tmp/config.json", READ_ONLY)
                .withClasspathResourceMapping(getTestPluginFilename(), "/vault/plugins/test-plugin", READ_ONLY)
                .withCommand("server", "-log-level=debug", "-config=" + TMP_VAULT_CONFIG_JSON_FILE);

        vaultContainer.setPortBindings(Arrays.asList(VAULT_PORT + ":" + VAULT_PORT));

        postgresContainer.start();
        execPostgres(format("psql -U %s -d %s -f %s", DB_USERNAME, DB_NAME, TMP_POSTGRES_INIT_SQL_FILE));

        rabbitMQContainer.start();

        Consumer<OutputFrame> consumer = outputFrame -> System.out.print("VAULT >> " + outputFrame.getUtf8String());
        vaultContainer.setLogConsumers(Arrays.asList(consumer));
        vaultContainer.start();

        initVault();
        log.info("vault has started with root token: " + rootToken);
    }

    private String getVaultImage() {
        return "hashicorp/vault:" + VaultVersions.VAULT_TEST_VERSION;
    }

    private void initVault() throws Exception {

        waitForContainerToStart();

        var vaultClient = createVaultClient();

        waitForVaultAPIToBeReady(vaultClient);

        var vaultInit = vaultClient.sys().init().init(new VaultSysInitParams()
                .setSecretShares(1)
                .setSecretThreshold(1))
                .toCompletableFuture().get();
        String unsealKey = vaultInit.getKeys().get(0);
        rootToken = vaultInit.getRootToken();

        vaultClient = vaultClient.configure().clientToken(rootToken).build();

        try {
            vaultClient.sys().health().statusCode(false, false)
                    .toCompletableFuture().get();
        } catch (VaultClientException e) {
            // https://www.vaultproject.io/api/system/health.html
            // 503 = sealed
            assertEquals(503, e.getStatus());
        }

        // unseal
        execVault("vault operator unseal " + unsealKey);

        var sealStatus = vaultClient.sys().seal().status().toCompletableFuture().get();
        assertFalse(sealStatus.isSealed());

        // userpass auth
        execVault("vault auth enable userpass");
        execVault(format("vault write auth/userpass/users/%s password=%s policies=%s",
                VAULT_AUTH_USERPASS_USER, VAULT_AUTH_USERPASS_PASSWORD, VAULT_POLICY));

        // k8s auth
        execVault("vault auth enable kubernetes");

        // approle auth
        execVault("vault auth enable approle");
        execVault(format("vault write auth/approle/role/%s policies=%s",
                VAULT_AUTH_APPROLE, VAULT_POLICY));
        appRoleSecretId = vaultClient.auth().appRole().generateSecretId(VAULT_AUTH_APPROLE, null)
                .toCompletableFuture().get()
                .getSecretId();
        appRoleRoleId = vaultClient.auth().appRole().readRoleId(VAULT_AUTH_APPROLE).toCompletableFuture().get();
        log.info(
                format("generated role_id=%s secret_id=%s for approle=%s", appRoleRoleId, appRoleSecretId, VAULT_AUTH_APPROLE));

        // policy
        String policyContent = readResourceContent("vault.policy");
        vaultClient.sys().policy().update(VAULT_POLICY, policyContent)
                .toCompletableFuture().get();

        // executable permission for test-plugin
        execVault("chmod +x /vault/plugins/test-plugin");

        // static secrets kv v1
        execVault(format("vault secrets enable -path=%s kv", SECRET_PATH_V1));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(
                format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, LIST_PATH + "/" + LIST_SUB_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));
        execVault(format("vault kv put %s/foo-json @/tmp/secret.json", SECRET_PATH_V1));
        execVault(format("vault kv put %s/config-json @/tmp/config.json", SECRET_PATH_V1));

        // static secrets kv v2
        execVault(format("vault secrets enable -path=%s -version=2 kv", SECRET_PATH_V2));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_SECRET_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(
                format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, LIST_PATH + "/" + LIST_SUB_PATH, SECRET_KEY, SECRET_VALUE));
        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, APP_CONFIG_PATH, PASSWORD_PROPERTY_NAME, DB_PASSWORD));
        execVault(format("vault kv put %s/foo-json @/tmp/secret.json", SECRET_PATH_V2));
        execVault(format("vault kv put %s/config-json @/tmp/config.json", SECRET_PATH_V2));

        // multi config
        execVault(format("vault kv put %s/multi/default1 color=blue size=XL", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/default2 color=red weight=3", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/singer1 firstname=paul lastname=shaffer", SECRET_PATH_V2));
        execVault(format("vault kv put %s/multi/singer2 lastname=simon age=78 color=green", SECRET_PATH_V2));

        // wrapped

        appRoleSecretIdWrappingToken = fetchWrappingToken(
                execVault(format("vault write -wrap-ttl=120s -f auth/approle/role/%s/secret-id", VAULT_AUTH_APPROLE)));
        log.info("appRoleSecretIdWrappingToken=" + appRoleSecretIdWrappingToken);

        clientTokenWrappingToken = fetchWrappingToken(
                execVault(format("vault token create -wrap-ttl=120s -ttl=10m -policy=%s", VAULT_POLICY)));
        log.info("clientTokenWrappingToken=" + clientTokenWrappingToken);

        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V1, WRAPPING_TEST_PATH, "password",
                VAULT_AUTH_USERPASS_PASSWORD));
        passwordKvv1WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V1, WRAPPING_TEST_PATH)));
        log.info("passwordKvv1WrappingToken=" + passwordKvv1WrappingToken);

        execVault(format("vault kv put %s/%s %s=%s", SECRET_PATH_V2, WRAPPING_TEST_PATH, "password",
                VAULT_AUTH_USERPASS_PASSWORD));
        passwordKvv2WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V2, WRAPPING_TEST_PATH)));
        log.info("passwordKvv2WrappingToken=" + passwordKvv2WrappingToken);
        anotherPasswordKvv2WrappingToken = fetchWrappingToken(
                execVault(format("vault kv get -wrap-ttl=120s %s/%s", SECRET_PATH_V2, WRAPPING_TEST_PATH)));
        log.info("anotherPasswordKvv2WrappingToken=" + anotherPasswordKvv2WrappingToken);

        // dynamic secrets

        execVault("vault secrets enable database");

        String vault_write_database_config_mydb = format(
                "vault write database/config/%s " +
                        "plugin_name=postgresql-database-plugin " +
                        "allowed_roles=%s " +
                        "connection_url=postgresql://{{username}}:{{password}}@%s:%s/%s?sslmode=disable " +
                        "username=%s " +
                        "password=%s",
                DB_NAME, VAULT_DBROLE, POSTGRESQL_HOST, POSTGRESQL_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD);
        execVault(vault_write_database_config_mydb);

        String vault_write_database_roles_mydbrole = format(
                "vault write database/roles/%s " +
                        "db_name=%s " +
                        "creation_statements=@%s " +
                        "default_ttl=%s " +
                        "max_ttl=%s " +
                        "revocation_statements=\"ALTER ROLE \\\"{{name}}\\\" NOLOGIN;\" " +
                        "renew_statements=\"ALTER ROLE \\\"{{name}}\\\" VALID UNTIL '{{expiration}}';\"",
                VAULT_DBROLE, DB_NAME, TMP_VAULT_POSTGRES_CREATION_SQL_FILE, db_default_ttl, db_max_ttl);
        execVault(vault_write_database_roles_mydbrole);

        execVault("vault secrets enable rabbitmq");

        String vault_write_rabbitmq_config = format(
                "vault write rabbitmq/config/connection " +
                        "connection_uri=http://%s:15672 " +
                        "username=%s " +
                        "password=%s",
                RABBITMQ_HOST, RMQ_USERNAME, RMQ_PASSWORD);
        execVault(vault_write_rabbitmq_config);

        String vault_write_rabbitmq_roles_myrabbitmqrole = format(
                "vault write rabbitmq/roles/%s " +
                        "vhosts='{\"/\":{\"configure\":\".*\", \"write\":\".*\", \"read\":\".*\"}}' " +
                        "default_ttl=%s " +
                        "max_ttl=%s",
                VAULT_RMQROLE, db_default_ttl, db_max_ttl);
        execVault(vault_write_rabbitmq_roles_myrabbitmqrole);

        // transit

        execVault("vault secrets enable transit");
        execVault(format("vault write -f transit/keys/%s", ENCRYPTION_KEY_NAME));
        execVault(format("vault write -f transit/keys/%s", ENCRYPTION_KEY2_NAME));
        execVault(format("vault write transit/keys/%s derived=true", ENCRYPTION_DERIVED_KEY_NAME));
        execVault(format("vault write transit/keys/%s type=ecdsa-p256", SIGN_KEY_NAME));
        execVault(format("vault write transit/keys/%s type=rsa-2048", SIGN_KEY2_NAME));
        execVault(format("vault write transit/keys/%s type=ed25519 derived=true", SIGN_DERIVATION_KEY_NAME));

        execVault("vault write transit/keys/jws type=ecdsa-p256");

        // TOTP

        execVault("vault secrets enable totp");

        // PKI

        execVault("vault secrets enable pki");
        execVault("vault secrets enable -path=pki2 pki");
        execVault("vault secrets tune -max-lease-ttl=8760h pki");
        execVault("vault token create -policy=root -id=pkiroot");
    }

    public static String readResourceContent(String path) throws IOException {
        return new String(readResourceData(path));
    }

    public static byte[] readResourceData(String path) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResourceAsStream(path).readAllBytes();
    }

    private String fetchWrappingToken(String out) {
        Pattern wrappingTokenPattern = Pattern.compile("^wrapping_token:\\s+([^\\s]+)$", MULTILINE);
        Matcher matcher = wrappingTokenPattern.matcher(out);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException("unable to find wrapping_token in " + out);
        }
    }

    public static String getTestPluginFilename() {
        return "vault-test-plugin-linux-" + getPluginArchitecture();
    }

    public static String getPluginArchitecture() {
        var osArch = System.getProperty("os.arch");
        return osArch.contains("aarch") || osArch.contains("arm") ? "arm64" : "amd64";
    }

    public static boolean useTls() {
        return System.getProperty("vault-test-extension.use-tls", TRUE.toString()).equals(TRUE.toString());
    }

    private void waitForContainerToStart() throws InterruptedException, IOException {
        Instant started = Instant.now();
        while (Instant.now().isBefore(started.plusSeconds(20))) {
            Container.ExecResult vault_status = vaultContainer.execInContainer(createVaultCommand("vault status"));
            int exitCode = vault_status.getExitCode();
            log.info("vault status exit code = " + exitCode + " (0=unsealed, 1=error, 2=sealed)");
            if (exitCode == 2) { // 2 => sealed
                return;
            }
            Thread.sleep(1000);
        }
        fail("vault failed to start");
    }

    private void waitForVaultAPIToBeReady(VaultClient vaultClient) throws Exception {
        Instant started = Instant.now();
        while (Instant.now().isBefore(started.plusSeconds(20))) {
            try {
                log.info("checking seal status");
                var sealStatus = vaultClient.sys().seal().status()
                        .toCompletableFuture().get();
                log.info(sealStatus);
                return;
            } catch (VaultException e) {
                log.info("vault api not ready: " + e);
            }
            Thread.sleep(1000L);
        }
        fail("vault failed to start");
    }

    private String execPostgres(String command) throws IOException, InterruptedException {
        String[] cmd = { "/bin/sh", "-c", command + " > " + CONTAINER_TMP_CMD + OUT_FILE };
        return exec(postgresContainer, command, cmd, HOST_POSTGRES_TMP_CMD + OUT_FILE);
    }

    private String execVault(String command) throws IOException, InterruptedException {
        String[] cmd = createVaultCommand(command + " > " + CONTAINER_TMP_CMD + OUT_FILE);
        return exec(vaultContainer, command, cmd, HOST_VAULT_TMP_CMD + OUT_FILE);
    }

    private String exec(GenericContainer container, String command, String[] cmd, String outFile)
            throws IOException, InterruptedException {
        exec(container, cmd);
        String out = Files.readString(Paths.get(outFile));
        log.info("> " + command + "\n" + out);
        return out;
    }

    private static Container.ExecResult exec(GenericContainer container, String[] cmd)
            throws IOException, InterruptedException {

        Container.ExecResult execResult = container.execInContainer(cmd);

        if (execResult.getExitCode() != 0) {
            throw new RuntimeException(
                    "command " + Arrays.asList(cmd) + " failed with exit code " + execResult.getExitCode() + "\n"
                            + execResult.getStderr());
        }
        return execResult;
    }

    private String[] createVaultCommand(String command) {
        String cmd = (rootToken != null ? "export VAULT_TOKEN=" + rootToken + " && " : "") + command;
        return new String[] { "/bin/sh", "-c", cmd };
    }

    public void close() {

        log.info("stop containers");

        if (vaultContainer != null && vaultContainer.isRunning()) {
            vaultContainer.stop();
        }
        if (postgresContainer != null && postgresContainer.isRunning()) {
            postgresContainer.stop();
        }
        if (rabbitMQContainer != null && rabbitMQContainer.isRunning()) {
            rabbitMQContainer.stop();
        }

        // VaultManager.getInstance().reset();
    }
}

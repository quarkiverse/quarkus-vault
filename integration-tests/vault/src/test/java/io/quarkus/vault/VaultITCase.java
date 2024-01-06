package io.quarkus.vault;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.DATABASE_DEFAULT_MOUNT;
import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.RABBITMQ_DEFAULT_MOUNT;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.APPROLE;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.USERPASS;
import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.DB_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.EXPECTED_SUB_PATHS;
import static io.quarkus.vault.test.VaultTestExtension.LIST_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V1;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V2;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_APPROLE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_PASSWORD;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_USERPASS_USER;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_DBROLE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_RMQROLE;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.config.spi.ConfigProviderResolver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.credentials.CredentialsProvider;
import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.common.VaultFormat;
import io.quarkus.vault.client.api.common.VaultHashAlgorithm;
import io.quarkus.vault.client.api.common.VaultLeasedResult;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretResult;
import io.quarkus.vault.client.api.secrets.transit.*;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultConfigSource;
import io.quarkus.vault.test.VaultTestExtension;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultITCase {

    private static final Logger log = Logger.getLogger(VaultITCase.class);

    public static final String MY_PASSWORD = "my-password";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultClient vaultClient;

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    CredentialsProvider credentialsProvider;

    @ConfigProperty(name = PASSWORD_PROPERTY_NAME)
    String someSecret;

    @ConfigProperty(name = MY_PASSWORD)
    String someSecretThroughIndirection;

    @Test
    public void credentialsProvider() {
        Map<String, String> staticCredentials = credentialsProvider.getCredentials("static");
        assertEquals("{" + PASSWORD_PROPERTY_NAME + "=" + DB_PASSWORD + "}", staticCredentials.toString());

        Map<String, String> dynamicCredentials = credentialsProvider.getCredentials("dynamic");
        String username = dynamicCredentials.get(USER_PROPERTY_NAME);
        assertTrue(username.startsWith("v-" + USERPASS.name().toLowerCase() + "-" + VAULT_DBROLE + "-"));
    }

    @Test
    public void config() {
        assertEquals(DB_PASSWORD, someSecret);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(PASSWORD_PROPERTY_NAME, String.class);
        assertEquals(DB_PASSWORD, value);

        int ordinal = StreamSupport.stream(config.getConfigSources().spliterator(), false)
                .filter(cs -> cs instanceof VaultConfigSource)
                .findAny()
                .orElseThrow(() -> new RuntimeException("vault config source not found"))
                .getOrdinal();

        Assertions.assertEquals(300, ordinal);
    }

    @Test
    public void configPropertyIndirection() {
        assertEquals(DB_PASSWORD, someSecretThroughIndirection);

        Config config = ConfigProviderResolver.instance().getConfig();
        String value = config.getValue(MY_PASSWORD, String.class);
        assertEquals(DB_PASSWORD, value);
    }

    @Test
    public void configPropertyScalar() {
        Config config = ConfigProviderResolver.instance().getConfig();
        assertTrue(config.getValue("my-enabled", Boolean.class));
        assertTrue(config.getOptionalValue("my-foo", String.class).isEmpty());
        assertTrue(config.getOptionalValue("my-fooList", String.class).isEmpty());
        assertTrue(config.getOptionalValue("my-fooMap", String.class).isEmpty());
    }

    @Test
    public void secret() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

    @Test
    public void crudSecret() {
        VaultTestExtension.assertCrudSecret(kvSecretEngine);
    }

    static class WrapExample {
        public String foo = "bar";
        public String zip = "zap";
    }

    @Test
    public void httpclient() {

        var anotherWrappingToken = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.another-password-kv-v2-wrapping-token", String.class);
        var unwrap = vaultClient.sys().wrapping()
                .unwrapAs(anotherWrappingToken, VaultSecretsKV2ReadSecretResult.class)
                .await().indefinitely();
        assertEquals(VAULT_AUTH_USERPASS_PASSWORD, unwrap.getData().getData().get("password"));
        try {
            vaultClient.sys().wrapping().unwrapAs(anotherWrappingToken, VaultSecretsKV2ReadSecretResult.class)
                    .await().indefinitely();
            fail("expected error 400: wrapping token is not valid or does not exist");
        } catch (VaultClientException e) {
            // fails on second unwrap attempt
            assertEquals(400, e.getStatus());
        }

        String appRoleRoleId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.role-id", String.class);
        String appRoleSecretId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.secret-id", String.class);
        var vaultAppRoleAuth = vaultClient.auth().appRole().login(appRoleRoleId, appRoleSecretId)
                .await().indefinitely();
        var appRoleClientToken = vaultAppRoleAuth.getClientToken();
        assertNotNull(appRoleClientToken);
        log.info("appRoleClientToken = " + appRoleClientToken);

        var appRoleClient = vaultClient.configure().clientToken(appRoleClientToken).build();

        assertTokenAppRole(appRoleClient);
        assertKvSecrets(appRoleClient);
        assertDynamicCredentials(appRoleClient, DATABASE_DEFAULT_MOUNT, VAULT_DBROLE, APPROLE);
        assertDynamicCredentials(appRoleClient, RABBITMQ_DEFAULT_MOUNT, VAULT_RMQROLE, APPROLE);
        assertWrap(appRoleClient);
        assertTransit(appRoleClient);

        var vaultUserPassAuth = vaultClient.auth().userPass().login(VAULT_AUTH_USERPASS_USER, VAULT_AUTH_USERPASS_PASSWORD)
                .await().indefinitely();
        String userPassClientToken = vaultUserPassAuth.getClientToken();
        log.info("userPassClientToken = " + userPassClientToken);
        assertNotNull(userPassClientToken);

        var userPassClient = vaultClient.configure().clientToken(userPassClientToken).build();

        assertTokenUserPass(userPassClient);
        assertKvSecrets(userPassClient);
        assertDynamicCredentials(userPassClient, DATABASE_DEFAULT_MOUNT, VAULT_DBROLE, USERPASS);
        assertDynamicCredentials(userPassClient, RABBITMQ_DEFAULT_MOUNT, VAULT_RMQROLE, USERPASS);
        assertWrap(userPassClient);
    }

    private void assertWrap(VaultClient vaultClient) {
        var wrapResult = vaultClient.sys().wrapping().wrap(new WrapExample(), Duration.ofMinutes(1))
                .await().indefinitely();
        WrapExample unwrapExample = vaultClient.sys().wrapping().unwrapAs(wrapResult.getToken(), WrapExample.class)
                .await().indefinitely();
        assertEquals("bar", unwrapExample.foo);
        assertEquals("zap", unwrapExample.zip);
    }

    private void assertTransit(VaultClient vaultClient) {

        var context = "mycontext".getBytes(StandardCharsets.UTF_8);

        assertTransitEncryption(vaultClient, ENCRYPTION_KEY_NAME, null);
        assertTransitEncryption(vaultClient, ENCRYPTION_KEY_NAME, context);

        assertTransitSign(vaultClient, SIGN_KEY_NAME, null);
        assertTransitSign(vaultClient, SIGN_KEY_NAME, context);

        vaultClient.secrets().transit().rotateKey(ENCRYPTION_KEY_NAME, null)
                .await().indefinitely();

        assertHash(vaultClient);
    }

    private void assertHash(VaultClient vaultClient) {
        var data = "coucou".getBytes(StandardCharsets.UTF_8);
        var hash = vaultClient.secrets().transit().hash(VaultHashAlgorithm.SHA2_512, data, VaultFormat.BASE64)
                .await().indefinitely();
        String expected = "4FrxOZ9PS+t5NMnxK6WpyI9+4ejvP+ehZ75Ll5xRXSQQKtkNOgdU1I/Fkw9jaaMIfmhulzLvNGDmQ5qVCJtIAA==";
        assertEquals(expected, hash);
    }

    private void assertTransitSign(VaultClient vaultClient, String keyName, byte[] context) {

        String data = "coucou";

        var signParams = new VaultSecretsTransitSignParams()
                .setInput(data.getBytes(StandardCharsets.UTF_8))
                .setContext(context);

        var sign = vaultClient.secrets().transit().sign(keyName, signParams)
                .await().indefinitely();

        var verifyParams = new VaultSecretsTransitVerifyParams()
                .setInput(data.getBytes(StandardCharsets.UTF_8))
                .setContext(context)
                .setSignature(sign.getSignature());

        var verify = vaultClient.secrets().transit().verify(keyName, verifyParams)
                .await().indefinitely();
        assertTrue(verify);
    }

    private void assertTransitEncryption(VaultClient vaultClient, String keyName, byte[] context) {

        String data = "coucou";

        var encryptParams = new VaultSecretsTransitEncryptParams()
                .setPlaintext(data.getBytes(StandardCharsets.UTF_8))
                .setContext(context);
        var encryptBatchResult = vaultClient.secrets().transit().encrypt(keyName, encryptParams)
                .await().indefinitely();
        String ciphertext = encryptBatchResult.getCiphertext();

        String batchDecryptedString = decrypt(vaultClient, keyName, ciphertext, context);
        assertEquals(data, batchDecryptedString);

        var rewrapParams = new VaultSecretsTransitRewrapParams()
                .setCiphertext(ciphertext)
                .setContext(context);
        var rewrap = vaultClient.secrets().transit().rewrap(keyName, rewrapParams)
                .await().indefinitely();
        String rewrappedCiphertext = rewrap.getCiphertext();

        batchDecryptedString = decrypt(vaultClient, keyName, rewrappedCiphertext, context);
        assertEquals(data, batchDecryptedString);
    }

    private String decrypt(VaultClient vaultClient, String keyName, String ciphertext, byte[] context) {
        var params = new VaultSecretsTransitDecryptParams()
                .setCiphertext(ciphertext)
                .setContext(context);
        var decryptBatchResult = vaultClient.secrets().transit().decrypt(keyName, params)
                .await().indefinitely();
        return new String(decryptBatchResult, StandardCharsets.UTF_8);
    }

    private void assertDynamicCredentials(VaultClient vaultClient, String mount, String role,
            VaultAuthenticationType authType) {

        VaultLeasedResult<?, ?> dynCreds;

        switch (mount) {
            case DATABASE_DEFAULT_MOUNT:
                var dbCreds = vaultClient.secrets().database(mount).generateCredentials(role)
                        .await().indefinitely();
                dynCreds = dbCreds;
                String dbUsername = Objects.toString(dbCreds.getData().get("username"));
                assertTrue(dbUsername.startsWith("v-" + authType.name().toLowerCase() + "-" + role + "-"));
                break;

            case RABBITMQ_DEFAULT_MOUNT:
                var rmqCreds = vaultClient.secrets().rabbitMQ(mount).generateCredentials(role)
                        .await().indefinitely();
                dynCreds = rmqCreds;
                String rmqUsername = rmqCreds.getData().getUsername();
                assertTrue(rmqUsername.startsWith(authType.name().toLowerCase() + "-"));
                assertDoesNotThrow(() -> UUID.fromString(rmqUsername.substring(rmqUsername.length() - 36)));
                break;

            default:
                throw new IllegalArgumentException("unknown mount: " + mount);
        }

        var vaultLeasesLookup = vaultClient.sys().leases().read(dynCreds.getLeaseId())
                .await().indefinitely();
        assertEquals(dynCreds.getLeaseId(), vaultLeasesLookup.getId());

        var vaultRenewLease = vaultClient.sys().leases().renew(dynCreds.getLeaseId(), null)
                .await().indefinitely();
        assertEquals(dynCreds.getLeaseId(), vaultRenewLease.getLeaseId());
    }

    private void assertKvSecrets(VaultClient vaultClient) {
        var kv1 = vaultClient.secrets().kv1(SECRET_PATH_V1);
        var kv2 = vaultClient.secrets().kv2(SECRET_PATH_V2);

        var secretV1 = kv1.read(APP_SECRET_PATH)
                .await().indefinitely();
        assertEquals(SECRET_VALUE, secretV1.get(SECRET_KEY));
        var vaultKvListSecretsV1 = kv1.list(LIST_PATH)
                .await().indefinitely();
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV1.toString());
        var fooJsonV1 = kv1.read("foo-json").await().indefinitely();
        assertEquals("{hello={foo=bar}}", fooJsonV1.toString());
        var configJsonV1 = kv1.read("config-json").await().indefinitely();
        assertTrue((boolean) configJsonV1.get("isEnabled"));

        var secretV2 = kv2.readSecret(APP_SECRET_PATH)
                .await().indefinitely();
        assertEquals(SECRET_VALUE, secretV2.getData().get(SECRET_KEY));
        var vaultKvListSecretsV2 = kv2.listSecrets(LIST_PATH).await().indefinitely();
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV2.toString());
        var fooJsonV2 = kv2.readSecret("foo-json").await().indefinitely();
        assertEquals("{hello={foo=bar}}", fooJsonV2.getData().toString());
        var configJsonV2 = kv2.readSecret("config-json").await().indefinitely();
        assertTrue((boolean) configJsonV2.getData().get("isEnabled"));
    }

    private void assertTokenUserPass(VaultClient vaultClient) {
        var vaultLookupSelf = vaultClient.auth().token().lookupSelf()
                .await().indefinitely();
        assertEquals("auth/" + USERPASS.name().toLowerCase() + "/login/" + VAULT_AUTH_USERPASS_USER, vaultLookupSelf.getPath());

        var vaultRenewSelf = vaultClient.auth().token().renewSelf(Duration.ofHours(1))
                .await().indefinitely();
        assertEquals(VAULT_AUTH_USERPASS_USER, vaultRenewSelf.getMetadata().get("username"));
    }

    private void assertTokenAppRole(VaultClient vaultClient) {
        var vaultLookupSelf = vaultClient.auth().token().lookupSelf()
                .await().indefinitely();
        assertEquals("auth/approle/login", vaultLookupSelf.getPath());

        var vaultRenewSelf = vaultClient.auth().token().renewSelf(Duration.ofHours(1))
                .await().indefinitely();
        assertEquals(VAULT_AUTH_APPROLE, vaultRenewSelf.getMetadata().get("role_name"));
    }
}

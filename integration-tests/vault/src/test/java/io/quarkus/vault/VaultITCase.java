package io.quarkus.vault;

import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;
import static io.quarkus.vault.runtime.VaultAuthManager.USERPASS_WRAPPING_TOKEN_PASSWORD_KEY;
import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.DATABASE_DEFAULT_MOUNT;
import static io.quarkus.vault.runtime.config.CredentialsProviderConfig.DEFAULT_REQUEST_PATH;
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
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Map;
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
import io.quarkus.vault.runtime.Base64String;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalAppRoleAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalTokenAuthMethod;
import io.quarkus.vault.runtime.client.authmethod.VaultInternalUserpassAuthMethod;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.VaultModel;
import io.quarkus.vault.runtime.client.dto.auth.VaultAppRoleAuth;
import io.quarkus.vault.runtime.client.dto.auth.VaultLookupSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultRenewSelf;
import io.quarkus.vault.runtime.client.dto.auth.VaultUserPassAuth;
import io.quarkus.vault.runtime.client.dto.dynamic.VaultDynamicCredentials;
import io.quarkus.vault.runtime.client.dto.kv.*;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultWrapResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBatchInput;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDynamicCredentialsSecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV1SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalKvV2SecretEngine;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTransitSecretEngine;
import io.quarkus.vault.runtime.config.VaultAuthenticationType;
import io.quarkus.vault.runtime.config.VaultConfigSource;
import io.quarkus.vault.test.VaultTestExtension;
import io.quarkus.vault.test.VaultTestLifecycleManager;
import io.quarkus.vault.test.client.TestVaultClient;
import io.quarkus.vault.test.client.dto.VaultTransitHash;
import io.quarkus.vault.test.client.dto.VaultTransitHashBody;

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

    @Inject
    VaultInternalKvV1SecretEngine vaultInternalKvV1SecretEngine;
    @Inject
    VaultInternalKvV2SecretEngine vaultInternalKvV2SecretEngine;
    @Inject
    VaultInternalTransitSecretEngine vaultInternalTransitSecretEngine;
    @Inject
    VaultInternalSystemBackend vaultInternalSystemBackend;
    @Inject
    VaultInternalAppRoleAuthMethod vaultInternalAppRoleAuthMethod;
    @Inject
    VaultInternalUserpassAuthMethod vaultInternalUserpassAuthMethod;
    @Inject
    VaultInternalTokenAuthMethod vaultInternalTokenAuthMethod;
    @Inject
    VaultInternalDynamicCredentialsSecretEngine vaultInternalDynamicCredentialsSecretEngine;

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

    static class WrapExample implements VaultModel {
        public String foo = "bar";
        public String zip = "zap";
    }

    @Test
    public void httpclient() {

        String anotherWrappingToken = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.another-password-kv-v2-wrapping-token", String.class);
        VaultKvSecretJsonV2 unwrap = vaultInternalSystemBackend
                .unwrap(vaultClient, anotherWrappingToken, VaultKvSecretJsonV2.class)
                .await()
                .indefinitely();
        assertEquals(VAULT_AUTH_USERPASS_PASSWORD, unwrap.data.data.get(USERPASS_WRAPPING_TOKEN_PASSWORD_KEY));
        try {
            vaultInternalSystemBackend.unwrap(vaultClient, anotherWrappingToken, VaultKvSecretJsonV2.class).await()
                    .indefinitely();
            fail("expected error 400: wrapping token is not valid or does not exist");
        } catch (VaultClientException e) {
            // fails on second unwrap attempt
            assertEquals(400, e.getStatus());
        }

        String appRoleRoleId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.role-id", String.class);
        String appRoleSecretId = ConfigProviderResolver.instance().getConfig()
                .getValue("vault-test.secret-id", String.class);
        VaultAppRoleAuth vaultAppRoleAuth = vaultInternalAppRoleAuthMethod.login(vaultClient, appRoleRoleId, appRoleSecretId)
                .await()
                .indefinitely();
        String appRoleClientToken = vaultAppRoleAuth.auth.clientToken;
        assertNotNull(appRoleClientToken);
        log.info("appRoleClientToken = " + appRoleClientToken);

        assertTokenAppRole(appRoleClientToken);
        assertKvSecrets(appRoleClientToken);
        assertDynamicCredentials(appRoleClientToken, DATABASE_DEFAULT_MOUNT, VAULT_DBROLE, APPROLE);
        assertDynamicCredentials(appRoleClientToken, RABBITMQ_DEFAULT_MOUNT, VAULT_RMQROLE, APPROLE);
        assertWrap(appRoleClientToken);

        VaultUserPassAuth vaultUserPassAuth = vaultInternalUserpassAuthMethod.login(vaultClient, VAULT_AUTH_USERPASS_USER,
                VAULT_AUTH_USERPASS_PASSWORD).await().indefinitely();
        String userPassClientToken = vaultUserPassAuth.auth.clientToken;
        log.info("userPassClientToken = " + userPassClientToken);
        assertNotNull(userPassClientToken);

        assertTransit(appRoleClientToken);

        assertTokenUserPass(userPassClientToken);
        assertKvSecrets(userPassClientToken);
        assertDynamicCredentials(userPassClientToken, DATABASE_DEFAULT_MOUNT, VAULT_DBROLE, USERPASS);
        assertDynamicCredentials(userPassClientToken, RABBITMQ_DEFAULT_MOUNT, VAULT_RMQROLE, USERPASS);
        assertWrap(userPassClientToken);
    }

    private void assertWrap(String token) {
        VaultWrapResult wrapResult = vaultInternalSystemBackend.wrap(vaultClient, token, 60, new WrapExample()).await()
                .indefinitely();
        WrapExample unwrapExample = vaultInternalSystemBackend.unwrap(vaultClient, wrapResult.wrapInfo.token, WrapExample.class)
                .await()
                .indefinitely();
        assertEquals("bar", unwrapExample.foo);
        assertEquals("zap", unwrapExample.zip);
    }

    private void assertTransit(String token) {

        Base64String context = Base64String.from("mycontext");

        assertTransitEncryption(token, ENCRYPTION_KEY_NAME, null);
        assertTransitEncryption(token, ENCRYPTION_KEY_NAME, context);

        assertTransitSign(token, SIGN_KEY_NAME, null);
        assertTransitSign(token, SIGN_KEY_NAME, context);

        new TestVaultClient().rotate(token, ENCRYPTION_KEY_NAME);

        assertHash(token);
    }

    private void assertHash(String token) {
        VaultTransitHashBody body = new VaultTransitHashBody();
        body.input = Base64String.from("coucou");
        body.format = "base64";
        VaultTransitHash hash = new TestVaultClient().hash(token, "sha2-512", body).await().indefinitely();
        Base64String sum = hash.data.sum;
        String expected = "4FrxOZ9PS+t5NMnxK6WpyI9+4ejvP+ehZ75Ll5xRXSQQKtkNOgdU1I/Fkw9jaaMIfmhulzLvNGDmQ5qVCJtIAA==";
        assertEquals(expected, sum.getValue());
    }

    private void assertTransitSign(String token, String keyName, Base64String context) {

        String data = "coucou";

        VaultTransitSignBody batchBody = new VaultTransitSignBody();
        batchBody.batchInput = singletonList(new VaultTransitSignBatchInput(Base64String.from(data), context));

        VaultTransitSign batchSign = vaultInternalTransitSecretEngine.sign(vaultClient, token, keyName, null, batchBody).await()
                .indefinitely();

        VaultTransitVerifyBody verifyBody = new VaultTransitVerifyBody();
        VaultTransitVerifyBatchInput batchInput = new VaultTransitVerifyBatchInput(Base64String.from(data), context);
        batchInput.signature = batchSign.data.batchResults.get(0).signature;
        verifyBody.batchInput = singletonList(batchInput);

        VaultTransitVerify verify = vaultInternalTransitSecretEngine.verify(vaultClient, token, keyName, null, verifyBody)
                .await()
                .indefinitely();
        assertEquals(1, verify.data.batchResults.size());
        assertTrue(verify.data.batchResults.get(0).valid);
    }

    private void assertTransitEncryption(String token, String keyName, Base64String context) {

        String data = "coucou";

        VaultTransitEncryptBatchInput encryptBatchInput = new VaultTransitEncryptBatchInput(Base64String.from(data), context);
        VaultTransitEncryptBody encryptBody = new VaultTransitEncryptBody();
        encryptBody.batchInput = singletonList(encryptBatchInput);
        VaultTransitEncrypt encryptBatchResult = vaultInternalTransitSecretEngine
                .encrypt(vaultClient, token, keyName, encryptBody).await()
                .indefinitely();
        String ciphertext = encryptBatchResult.data.batchResults.get(0).ciphertext;

        String batchDecryptedString = decrypt(token, keyName, ciphertext, context);
        assertEquals(data, batchDecryptedString);

        VaultTransitRewrapBatchInput rewrapBatchInput = new VaultTransitRewrapBatchInput(ciphertext, context);
        VaultTransitRewrapBody rewrapBody = new VaultTransitRewrapBody();
        rewrapBody.batchInput = singletonList(rewrapBatchInput);
        VaultTransitEncrypt rewrap = vaultInternalTransitSecretEngine.rewrap(vaultClient, token, keyName, rewrapBody).await()
                .indefinitely();
        assertEquals(1, rewrap.data.batchResults.size());
        String rewrappedCiphertext = rewrap.data.batchResults.get(0).ciphertext;

        batchDecryptedString = decrypt(token, keyName, rewrappedCiphertext, context);
        assertEquals(data, batchDecryptedString);
    }

    private String decrypt(String token, String keyName, String ciphertext, Base64String context) {
        VaultTransitDecryptBatchInput decryptBatchInput = new VaultTransitDecryptBatchInput(ciphertext, context);
        VaultTransitDecryptBody decryptBody = new VaultTransitDecryptBody();
        decryptBody.batchInput = singletonList(decryptBatchInput);
        VaultTransitDecrypt decryptBatchResult = vaultInternalTransitSecretEngine
                .decrypt(vaultClient, token, keyName, decryptBody).await()
                .indefinitely();
        return decryptBatchResult.data.batchResults.get(0).plaintext.decodeAsString();
    }

    private void assertDynamicCredentials(String clientToken, String mount, String role, VaultAuthenticationType authType) {
        VaultDynamicCredentials vaultDynamicCredentials = vaultInternalDynamicCredentialsSecretEngine.generateCredentials(
                vaultClient, clientToken, mount, DEFAULT_REQUEST_PATH, role).await().indefinitely();

        switch (mount) {
            case DATABASE_DEFAULT_MOUNT:
                String dbUsername = vaultDynamicCredentials.data.username;
                assertTrue(dbUsername.startsWith("v-" + authType.name().toLowerCase() + "-" + role + "-"));
                break;

            case RABBITMQ_DEFAULT_MOUNT:
                String rmqUsername = vaultDynamicCredentials.data.username;
                assertTrue(rmqUsername.startsWith(authType.name().toLowerCase() + "-"));
                assertDoesNotThrow(() -> UUID.fromString(rmqUsername.substring(rmqUsername.length() - 36)));
                break;
        }

        VaultLeasesLookup vaultLeasesLookup = vaultInternalSystemBackend.lookupLease(vaultClient, clientToken,
                vaultDynamicCredentials.leaseId).await().indefinitely();
        assertEquals(vaultDynamicCredentials.leaseId, vaultLeasesLookup.data.id);

        VaultRenewLease vaultRenewLease = vaultInternalSystemBackend
                .renewLease(vaultClient, clientToken, vaultDynamicCredentials.leaseId)
                .await().indefinitely();
        assertEquals(vaultDynamicCredentials.leaseId, vaultRenewLease.leaseId);
    }

    private void assertKvSecrets(String clientToken) {
        VaultKvSecretJsonV1 secretV1 = vaultInternalKvV1SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V1, APP_SECRET_PATH).await()
                .indefinitely();
        assertEquals(SECRET_VALUE, secretV1.data.get(SECRET_KEY));
        VaultKvListSecrets vaultKvListSecretsV1 = vaultInternalKvV1SecretEngine
                .listSecrets(vaultClient, clientToken, SECRET_PATH_V1,
                        LIST_PATH)
                .await().indefinitely();
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV1.data.keys.toString());
        VaultKvSecretJsonV1 fooJsonV1 = vaultInternalKvV1SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V1, "foo-json").await().indefinitely();
        assertEquals("{hello={foo=bar}}", fooJsonV1.data.toString());
        VaultKvSecretJsonV1 configJsonV1 = vaultInternalKvV1SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V1, "config-json").await().indefinitely();
        assertTrue((boolean) configJsonV1.data.get("isEnabled"));

        VaultKvSecretJsonV2 secretV2 = vaultInternalKvV2SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V2, APP_SECRET_PATH).await()
                .indefinitely();
        assertEquals(SECRET_VALUE, secretV2.data.data.get(SECRET_KEY));
        VaultKvListSecrets vaultKvListSecretsV2 = vaultInternalKvV2SecretEngine
                .listSecrets(vaultClient, clientToken, SECRET_PATH_V2,
                        LIST_PATH)
                .await().indefinitely();
        assertEquals(EXPECTED_SUB_PATHS, vaultKvListSecretsV2.data.keys.toString());
        VaultKvSecretJsonV2 fooJsonV2 = vaultInternalKvV2SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V2, "foo-json").await().indefinitely();
        assertEquals("{hello={foo=bar}}", fooJsonV2.data.data.toString());
        VaultKvSecretJsonV2 configJsonV2 = vaultInternalKvV2SecretEngine
                .getSecretJson(vaultClient, clientToken, SECRET_PATH_V2, "config-json").await().indefinitely();
        assertTrue((boolean) configJsonV2.data.data.get("isEnabled"));
    }

    private void assertTokenUserPass(String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultInternalTokenAuthMethod.lookupSelf(vaultClient, clientToken).await()
                .indefinitely();
        assertEquals("auth/" + USERPASS.name().toLowerCase() + "/login/" + VAULT_AUTH_USERPASS_USER, vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultInternalTokenAuthMethod.renewSelf(vaultClient, clientToken, "1h").await()
                .indefinitely();
        assertEquals(VAULT_AUTH_USERPASS_USER, vaultRenewSelf.auth.metadata.get("username"));
    }

    private void assertTokenAppRole(String clientToken) {
        VaultLookupSelf vaultLookupSelf = vaultInternalTokenAuthMethod.lookupSelf(vaultClient, clientToken).await()
                .indefinitely();
        assertEquals("auth/approle/login", vaultLookupSelf.data.path);

        VaultRenewSelf vaultRenewSelf = vaultInternalTokenAuthMethod.renewSelf(vaultClient, clientToken, "1h").await()
                .indefinitely();
        assertEquals(VAULT_AUTH_APPROLE, vaultRenewSelf.auth.metadata.get("role_name"));
    }
}

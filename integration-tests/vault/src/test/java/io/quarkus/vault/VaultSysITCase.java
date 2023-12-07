package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V1;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V2;
import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.EngineListingVisibility;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;
import io.quarkus.vault.test.VaultTestExtension;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultSysITCase {

    private static final Logger log = Logger.getLogger(VaultSysITCase.class.getName());

    public static final Random RANDOM = new Random();

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault.properties", "application.properties"));

    @Inject
    VaultSystemBackendEngine vaultSystemBackendEngine;

    @BeforeEach
    void setup() {
        // Unregister any test plugins that may have been registered by previous tests
        vaultSystemBackendEngine.removePlugin("secret", "test-plugin", null);
        vaultSystemBackendEngine.removePlugin("secret", "test-plugin", "v0.0.1");
    }

    @Test
    public void testSealStatus() {
        final VaultSealStatus vaultSealStatus = vaultSystemBackendEngine.sealStatus();
        assertThat(vaultSealStatus).returns("shamir", VaultSealStatus::getType);
        assertThat(vaultSealStatus).returns(false, VaultSealStatus::isSealed);
    }

    @Test
    public void policy() {
        String rules = "path \"transit/*\" {\n" +
                "  capabilities = [ \"create\", \"read\", \"update\" ]\n" +
                "}";
        String name = "sys-test-policy";
        vaultSystemBackendEngine.createUpdatePolicy(name, rules);
        List<String> policies = vaultSystemBackendEngine.getPolicies();
        assertTrue(policies.contains(name));
        String policyRules = vaultSystemBackendEngine.getPolicyRules(name);
        assertEquals(rules, policyRules);
        vaultSystemBackendEngine.deletePolicy(name);
        policies = vaultSystemBackendEngine.getPolicies();
        assertFalse(policies.contains(name));
    }

    @Test
    public void testTuneInfo() {
        VaultTuneInfo tuneInfo = vaultSystemBackendEngine.getTuneInfo("secret");
        assertNotNull(tuneInfo.getDescription());
        assertNotNull(tuneInfo.getDefaultLeaseTimeToLive());
        assertNotNull(tuneInfo.getMaxLeaseTimeToLive());
        assertNotNull(tuneInfo.getForceNoCache());

        VaultTuneInfo tuneInfoUpdates = new VaultTuneInfo();
        tuneInfoUpdates.setMaxLeaseTimeToLive(tuneInfo.getMaxLeaseTimeToLive() + 10);
        vaultSystemBackendEngine.updateTuneInfo("secret", tuneInfoUpdates);

        VaultTuneInfo updatedTuneInfo = vaultSystemBackendEngine.getTuneInfo("secret");

        assertEquals(tuneInfo.getMaxLeaseTimeToLive() + 10, updatedTuneInfo.getMaxLeaseTimeToLive());
    }

    @Test
    public void testSecretEngineInfo() {

        // v2
        VaultSecretEngineInfo info = vaultSystemBackendEngine.getSecretEngineInfo(SECRET_PATH_V2);
        Assertions.assertEquals(VaultSecretEngine.KEY_VALUE.getType(), info.getType());
        Assertions.assertEquals("", info.getDescription());
        Assertions.assertEquals("{version=2}", info.getOptions().toString());
        Assertions.assertFalse(info.getLocal());
        Assertions.assertFalse(info.getExternalEntropyAccess());
        Assertions.assertFalse(info.getSealWrap());

        // v1
        info = vaultSystemBackendEngine.getSecretEngineInfo(SECRET_PATH_V1);
        Assertions.assertEquals(VaultSecretEngine.KEY_VALUE.getType(), info.getType());
        assertNull(info.getOptions());
    }

    @Test
    public void testSecretEngineInfoNotFound() {
        try {
            vaultSystemBackendEngine.getSecretEngineInfo("unknown");
            Assertions.fail();
        } catch (VaultClientException e) {
            Assertions.assertEquals(400, e.getStatus());
        }
    }

    @Test
    public void testSecretEngineInfoPriorVault1_10() {
        try {
            vaultSystemBackendEngine.getSecretEngineInfo("secret");
            log.info("running on vault >= 1.10");
        } catch (VaultClientException e) {
            Assertions.assertEquals(405, e.getStatus());
        }
    }

    @Test
    public void testEnableDisable() {
        String randomMount = String.format("pki-%X", RANDOM.nextInt());

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));

        EnableEngineOptions options = new EnableEngineOptions();
        assertDoesNotThrow(
                () -> vaultSystemBackendEngine.enable(VaultSecretEngine.PKI, randomMount, "Dynamic PKI engine", options));

        assertTrue(vaultSystemBackendEngine.isEngineMounted(randomMount));

        assertDoesNotThrow(() -> vaultSystemBackendEngine.disable(randomMount));

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));
    }

    @Test
    public void testEnableAdvancedOptions() throws Exception {

        String randomMount = String.format("test-%X", RANDOM.nextInt());

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));

        EnableEngineOptions options = new EnableEngineOptions()
                .setOptions(Map.of("version", "2"))
                .setAllowedManagedKeys(List.of("key1"))
                .setAuditNonHMACRequestKeys(List.of("key2"))
                .setAuditNonHMACResponseKeys(List.of("key3"))
                .setAllowedResponseHeaders(List.of("header1"))
                .setPassthroughRequestHeaders(List.of("header2"))
                .setListingVisibility(EngineListingVisibility.UNAUTH)
                .setDefaultLeaseTimeToLive("1h")
                .setMaxLeaseTimeToLive("24h")
                .setForceNoCache(true);

        assertDoesNotThrow(
                () -> vaultSystemBackendEngine.enable("kv", randomMount, "KV with crazy options", options));

        var info = vaultSystemBackendEngine.getSecretEngineInfo(randomMount);
        assertEquals(List.of("key1"), info.getAllowedManagedKeys());
        assertEquals(List.of("key2"), info.getAuditNonHMACRequestKeys());
        assertEquals(List.of("key3"), info.getAuditNonHMACResponseKeys());
        assertEquals(List.of("header1"), info.getAllowedResponseHeaders());
        assertEquals(List.of("header2"), info.getPassthroughRequestHeaders());
        assertEquals(EngineListingVisibility.UNAUTH, info.getListingVisibility());
        assertEquals(3600, info.getDefaultLeaseTimeToLive());
        assertEquals(86400, info.getMaxLeaseTimeToLive());
        assertTrue(info.getForceNoCache());
        assertEquals("{version=2}", info.getOptions().toString());

        var tuneInfo = vaultSystemBackendEngine.getTuneInfo(randomMount);
        assertEquals(List.of("key1"), tuneInfo.getAllowedManagedKeys());
        assertEquals(List.of("key2"), tuneInfo.getAuditNonHMACRequestKeys());
        assertEquals(List.of("key3"), tuneInfo.getAuditNonHMACResponseKeys());
        assertEquals(List.of("header1"), tuneInfo.getAllowedResponseHeaders());
        assertEquals(List.of("header2"), tuneInfo.getPassthroughRequestHeaders());
        assertEquals(EngineListingVisibility.UNAUTH, tuneInfo.getListingVisibility());
        assertEquals(3600, tuneInfo.getDefaultLeaseTimeToLive());
        assertEquals(86400, tuneInfo.getMaxLeaseTimeToLive());
        assertTrue(tuneInfo.getForceNoCache());
        assertEquals("{version=2}", tuneInfo.getOptions().toString());
    }

    @Test
    public void testEnableDisableCustomPluginWithoutVersion() throws Exception {
        registerTestPlugin(false);

        String randomMount = String.format("test-%X", RANDOM.nextInt());

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));

        EnableEngineOptions options = new EnableEngineOptions()
                .setPluginVersion(null);

        assertDoesNotThrow(
                () -> vaultSystemBackendEngine.enable("test-plugin", randomMount, "Test plugin engine", options));

        var info = vaultSystemBackendEngine.getSecretEngineInfo(randomMount);
        assertEquals("", info.getPluginVersion());

        assertDoesNotThrow(() -> vaultSystemBackendEngine.disable(randomMount));

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));
    }

    @Test
    public void testEnableDisableCustomPluginWithVersion() throws Exception {
        registerTestPlugin(true);

        String randomMount = String.format("test-%X", RANDOM.nextInt());

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));

        EnableEngineOptions options = new EnableEngineOptions()
                .setPluginVersion("v0.0.1");

        assertDoesNotThrow(
                () -> vaultSystemBackendEngine.enable("test-plugin", randomMount, "Test plugin engine", options));

        assertTrue(vaultSystemBackendEngine.isEngineMounted(randomMount));

        assertDoesNotThrow(() -> vaultSystemBackendEngine.disable(randomMount));

        assertFalse(vaultSystemBackendEngine.isEngineMounted(randomMount));
    }

    @Test
    public void testListPlugins() throws Exception {
        var testPluginSHA = registerTestPlugin(true);

        var plugins = vaultSystemBackendEngine.listPlugins();
        assertNotNull(plugins);
        assertNotNull(plugins.getAuth());
        assertNotNull(plugins.getDatabase());
        assertNotNull(plugins.getSecret());
        assertNotNull(plugins.getDetailed());

        assertTrue(plugins.getAuth().contains("userpass"));
        assertTrue(plugins.getDatabase().contains("mysql-database-plugin"));
        assertTrue(plugins.getSecret().contains("kv"));

        // Check builtin plugin details
        var kvDetails = plugins.getDetailed().stream().filter(p -> p.getName().equals("kv")).findFirst().orElse(null);
        assertNotNull(kvDetails);
        assertEquals(true, kvDetails.getBuiltin());
        assertEquals("supported", kvDetails.getDeprecationStatus());
        assertEquals("kv", kvDetails.getName());
        assertEquals("secret", kvDetails.getType());
        assertTrue(kvDetails.getVersion().matches("v[0-9]+\\.[0-9]+\\.[0-9]+\\+builtin"));
        assertNull(kvDetails.getSha256());
        assertNull(kvDetails.getCommand());
        assertNull(kvDetails.getArgs());
        assertNull(kvDetails.getEnv());

        // Check custom plugin details
        var testPluginDetails = plugins.getDetailed().stream().filter(p -> p.getName().equals("test-plugin")).findFirst()
                .orElse(null);
        assertNotNull(testPluginDetails);
        assertEquals(false, testPluginDetails.getBuiltin());
        assertNull(testPluginDetails.getDeprecationStatus());
        assertEquals("test-plugin", testPluginDetails.getName());
        assertEquals("secret", testPluginDetails.getType());
        assertEquals("v0.0.1", testPluginDetails.getVersion());
        assertEquals(testPluginSHA, testPluginDetails.getSha256());
        assertNull(testPluginDetails.getCommand());
        assertNull(testPluginDetails.getArgs());
        assertNull(testPluginDetails.getEnv());
    }

    @Test
    void testListPluginsOfType() {
        var plugins = vaultSystemBackendEngine.listPlugins("secret");
        assertNotNull(plugins);
        assertTrue(plugins.contains("kv"));
        assertFalse(plugins.contains("userpass"));
    }

    @Test
    void testGetBuiltinPluginDetails() {
        var details = vaultSystemBackendEngine.getPluginDetails("secret", "kv", null);
        assertNotNull(details);
        assertEquals(true, details.getBuiltin());
        assertEquals("supported", details.getDeprecationStatus());
        assertEquals("kv", details.getName());
        assertNull(details.getType());
        assertTrue(details.getVersion().matches("v[0-9]+\\.[0-9]+\\.[0-9]+\\+builtin"));
        assertEquals("", details.getSha256());
        assertEquals("", details.getCommand());
        assertNull(details.getArgs());
        assertNull(details.getEnv());
    }

    @Test
    void testGetCustomPluginDetailsWithoutVersionQuery() throws Exception {
        var testPluginSHA = registerTestPlugin(false);

        var details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", null);
        assertNotNull(details);
        assertEquals(false, details.getBuiltin());
        assertNull(details.getDeprecationStatus());
        assertEquals("test-plugin", details.getName());
        assertNull(details.getType());
        assertEquals("", details.getVersion());
        assertEquals(testPluginSHA, details.getSha256());
        assertEquals("test-plugin", details.getCommand());
        assertEquals(List.of("--some-flag=1"), details.getArgs());
        // TODO: env vars are not returned by Vault yet
        // assertEquals(List.of("ENV_VAR=1"), testPluginDetails.getEnv());
        assertNull(details.getEnv());

        // Can't get details of an un-versioned custom plugin when specifying a version
        assertNull(vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", "v0.0.1"));
    }

    @Test
    void testGetCustomPluginDetailsWithVersionQuery() throws Exception {
        var testPluginSHA = registerTestPlugin(true);

        var details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", "v0.0.1");
        assertNotNull(details);
        assertEquals(false, details.getBuiltin());
        assertNull(details.getDeprecationStatus());
        assertEquals("test-plugin", details.getName());
        assertNull(details.getType());
        assertEquals("v0.0.1", details.getVersion());
        assertEquals(testPluginSHA, details.getSha256());
        assertEquals("test-plugin", details.getCommand());
        assertEquals(List.of("--some-flag=1"), details.getArgs());
        // TODO: env vars are not returned by Vault yet
        // assertEquals(List.of("ENV_VAR=1"), testPluginDetails.getEnv());
        assertNull(details.getEnv());

        // Can't get details of a versioned custom plugin without specifying a version
        assertNull(vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", null));
    }

    @Test
    void deregisterPluginWithoutVersion() throws Exception {
        registerTestPlugin(false);

        var details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", null);
        assertNotNull(details);

        vaultSystemBackendEngine.removePlugin("secret", "test-plugin", null);

        details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", null);
        assertNull(details);
    }

    @Test
    void deregisterPluginWithVersion() throws Exception {
        registerTestPlugin(true);

        var details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", "v0.0.1");
        assertNotNull(details);

        vaultSystemBackendEngine.removePlugin("secret", "test-plugin", "v0.0.1");

        details = vaultSystemBackendEngine.getPluginDetails("secret", "test-plugin", "v0.0.1");
        assertNull(details);
    }

    String registerTestPlugin(Boolean includeVersion) throws Exception {
        var testPluginSHA = sha256Hex(VaultTestExtension.readResourceData(VaultTestExtension.getTestPluginFilename()));
        var version = includeVersion ? "v0.0.1" : null;
        vaultSystemBackendEngine.registerPlugin("secret", "test-plugin", version, testPluginSHA, "test-plugin",
                List.of("--some-flag=1"), List.of("ENV_VAR=1"));
        return testPluginSHA;
    }
}

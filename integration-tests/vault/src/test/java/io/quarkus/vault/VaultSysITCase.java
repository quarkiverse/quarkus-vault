package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V1;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_PATH_V2;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;

import javax.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;
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
        Assertions.assertNull(info.getOptions());
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

}

package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static io.quarkus.vault.test.VaultTestExtension.VAULT_AUTH_APPROLE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.auth.VaultAppRoleAuthRole;
import io.quarkus.vault.auth.VaultAppRoleSecretId;
import io.quarkus.vault.auth.VaultAppRoleSecretIdAccessor;
import io.quarkus.vault.auth.VaultAppRoleSecretIdRequest;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultAppRoleITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-approle.properties", "application.properties"));

    private static final String TEST_APP_ROLE = "testAppRole";

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Inject
    VaultAppRoleAuthService vaultAppRoleAuthService;

    @Test
    public void secretV2() {
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

    @Test
    public void getAppRole() {
        VaultAppRoleAuthRole appRole = vaultAppRoleAuthService.getAppRole(VAULT_AUTH_APPROLE);
        assertNotNull(appRole);
    }

    @Test
    public void getCRUDAppRoleWithDefaults() {

        // creates with defaults
        VaultAppRoleAuthRole newAppRoleWithDefaults = new VaultAppRoleAuthRole();
        vaultAppRoleAuthService.createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaults);

        // list app roles
        List<String> appRoles = vaultAppRoleAuthService.getAppRoles();
        assertNotNull(appRoles);
        assertEquals(2, appRoles.size());

        // retrieve app role and checks defaults values have been set
        VaultAppRoleAuthRole appRoleAuthRole = vaultAppRoleAuthService.getAppRole(TEST_APP_ROLE);
        assertNotNull(appRoleAuthRole);
        assertTrue(appRoleAuthRole.bindSecretId);
        assertNull(appRoleAuthRole.secretIdBoundCidrs);
        assertEquals(0, appRoleAuthRole.secretIdNumUses);
        assertEquals("0", appRoleAuthRole.secretIdTtl);
        assertFalse(appRoleAuthRole.localSecretIds);
        assertEquals(0, appRoleAuthRole.tokenTtl);
        assertEquals(0, appRoleAuthRole.tokenMaxTtl);
        assertNotNull(appRoleAuthRole.tokenPolicies);
        assertEquals(0, appRoleAuthRole.tokenPolicies.size());
        assertNotNull(appRoleAuthRole.tokenBoundCidrs);
        assertEquals(0, appRoleAuthRole.tokenBoundCidrs.size());
        assertEquals(0, appRoleAuthRole.tokenExplicitMaxTtl);
        assertFalse(appRoleAuthRole.tokenNoDefaultPolicy);
        assertEquals(0, appRoleAuthRole.tokenNumUses);
        assertEquals(0, appRoleAuthRole.tokenPeriod);
        assertEquals("default", appRoleAuthRole.tokenType);

        // update approle
        VaultAppRoleAuthRole newAppRoleWithDefaultsOverriden = new VaultAppRoleAuthRole()
                .setBindSecretId(false)
                .setSecretIdBoundCidrs(Arrays.asList("192.168.1.1/24", "192.168.0.1/24"))
                .setSecretIdNumUses(5)
                .setSecretIdTtl("60m")
                .setTokenTtl(10)
                .setTokenMaxTtl(15)
                .setTokenPolicies(Arrays.asList("policy1", "policy2"))
                .setTokenBoundCidrs(Arrays.asList("192.168.0.0/24", "192.168.1.0/24"))
                .setTokenExplicitMaxTtl(20)
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(1)
                .setTokenPeriod(30)
                .setTokenType("service");
        vaultAppRoleAuthService.createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaultsOverriden);

        // list app roles
        assertNotNull(appRoles);
        assertEquals(2, appRoles.size());

        // retrieve app role and checks defaults values have been overriden
        appRoleAuthRole = vaultAppRoleAuthService.getAppRole(TEST_APP_ROLE);
        assertNotNull(appRoleAuthRole);
        assertFalse(appRoleAuthRole.bindSecretId);
        assertNotNull(appRoleAuthRole.secretIdBoundCidrs);
        assertEquals(2, appRoleAuthRole.secretIdBoundCidrs.size());
        assertEquals(5, appRoleAuthRole.secretIdNumUses);
        assertEquals("3600", appRoleAuthRole.secretIdTtl);
        assertFalse(appRoleAuthRole.localSecretIds);
        assertEquals(10, appRoleAuthRole.tokenTtl);
        assertEquals(15, appRoleAuthRole.tokenMaxTtl);
        assertNotNull(appRoleAuthRole.tokenPolicies);
        assertEquals(2, appRoleAuthRole.tokenPolicies.size());
        assertNotNull(appRoleAuthRole.tokenBoundCidrs);
        assertEquals(2, appRoleAuthRole.tokenBoundCidrs.size());
        assertEquals(20, appRoleAuthRole.tokenExplicitMaxTtl);
        assertTrue(appRoleAuthRole.tokenNoDefaultPolicy);
        assertEquals(1, appRoleAuthRole.tokenNumUses);
        assertEquals(30, appRoleAuthRole.tokenPeriod);
        assertEquals("service", appRoleAuthRole.tokenType);

        // retrieve role ID
        String roleId = vaultAppRoleAuthService.getAppRoleRoleId(TEST_APP_ROLE);
        assertNotNull(roleId);

        // force role ID
        String uuid = UUID.randomUUID().toString();
        vaultAppRoleAuthService.setAppRoleRoleId(TEST_APP_ROLE, String.valueOf(uuid));
        String newRoleId = vaultAppRoleAuthService.getAppRoleRoleId(TEST_APP_ROLE);
        assertEquals(uuid, newRoleId);

        // creates secret ID
        // need to set bind secret ID flag
        vaultAppRoleAuthService
                .createOrUpdateAppRole(TEST_APP_ROLE, newAppRoleWithDefaultsOverriden.setBindSecretId(Boolean.TRUE));
        VaultAppRoleSecretId secretId = vaultAppRoleAuthService
                .createNewSecretId(TEST_APP_ROLE, new VaultAppRoleSecretIdRequest());
        assertNotNull(secretId);
        assertNotNull(secretId.secretId);
        assertNotNull(secretId.secretIdAccessor);

        // verify that new secretAccesssorId apears in list
        List<String> secretIdAccessors = vaultAppRoleAuthService.getSecretIdAccessors(TEST_APP_ROLE);
        assertNotNull(secretIdAccessors);
        assertTrue(secretIdAccessors.contains(secretId.secretIdAccessor));

        // try to retrieve secret id
        VaultAppRoleSecretIdAccessor secretIdRetrieved = vaultAppRoleAuthService
                .getSecretId(TEST_APP_ROLE, secretId.secretId);
        assertEquals(secretIdRetrieved.secretIdAccessor, secretIdAccessors.get(0));

        // creates custom secret ID
        VaultAppRoleSecretId customSecretId = vaultAppRoleAuthService
                .createCustomSecretId(TEST_APP_ROLE, new VaultAppRoleSecretIdRequest().setSecretId("HelloWorld"));
        assertNotNull(customSecretId);
        assertEquals("HelloWorld", customSecretId.secretId);
        assertNotNull(customSecretId.secretIdAccessor);

        // verify that new secretAccesssorId apears in list
        secretIdAccessors = vaultAppRoleAuthService.getSecretIdAccessors(TEST_APP_ROLE);
        assertNotNull(secretIdAccessors);
        assertTrue(secretIdAccessors.contains(customSecretId.secretIdAccessor));

        // delete secret id
        vaultAppRoleAuthService.deleteSecretId(TEST_APP_ROLE, secretId.secretId);

        assertNull(vaultAppRoleAuthService.getSecretId(TEST_APP_ROLE, secretId.secretId));
    }

}

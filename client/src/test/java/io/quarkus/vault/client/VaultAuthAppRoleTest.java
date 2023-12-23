package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleCreateCustomSecretIdParams;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "approle", path = "approle")
})
public class VaultAuthAppRoleTest {

    @Test
    public void testLogin(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Read role id
        var roleId = appRoleApi.readRoleId(role)
                .await().indefinitely();

        assertThat(roleId)
                .isNotNull();

        // Generate secret id
        var secretId = appRoleApi.generateSecretId(role, null)
                .await().indefinitely().secretId;

        assertThat(secretId)
                .isNotNull();

        // Login
        var loginResult = appRoleApi.login(roleId, secretId)
                .await().indefinitely();

        assertThat(loginResult.clientToken)
                .isNotNull();
    }

    @Test
    public void testListRoles(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // List roles
        var roles = appRoleApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .isNotNull()
                .contains(role);
    }

    @Test
    public void testReadRole(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Read role
        var roleInfo = appRoleApi.readRole(role)
                .await().indefinitely();

        assertThat(roleInfo)
                .isNotNull();
        assertThat(roleInfo.bindSecretId)
                .isTrue();
        assertThat(roleInfo.tokenBoundCidrs)
                .isEmpty();
        assertThat(roleInfo.tokenExplicitMaxTtl)
                .isEqualTo("0");
        assertThat(roleInfo.tokenMaxTtl)
                .isEqualTo("0");
        assertThat(roleInfo.tokenNoDefaultPolicy)
                .isFalse();
        assertThat(roleInfo.tokenNumUses)
                .isEqualTo(0);
        assertThat(roleInfo.tokenPeriod)
                .isEqualTo(0);
        assertThat(roleInfo.tokenPolicies)
                .isEmpty();
        assertThat(roleInfo.tokenType)
                .isEqualTo("default");
    }

    @Test
    public void testDeleteRole(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Validate creation
        var roles = appRoleApi.listRoles()
                .await().indefinitely();
        assertThat(roles)
                .isNotNull()
                .contains(role);

        // Delete role
        appRoleApi.deleteRole(role)
                .await().indefinitely();

        // Validate deletion
        roles = appRoleApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .doesNotContain(role);
    }

    @Test
    public void testReadRoleId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Read role id
        var roleId = appRoleApi.readRoleId(role)
                .await().indefinitely();

        assertThat(roleId)
                .isNotNull();
    }

    @Test
    public void testUpdateRoleId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Read role id
        var roleId = appRoleApi.readRoleId(role)
                .await().indefinitely();

        assertThat(roleId)
                .isNotNull();

        // Update role id
        var customRoleId = role + "custom-role-id";
        appRoleApi.updateRoleId(role, customRoleId)
                .await().indefinitely();

        roleId = appRoleApi.readRoleId(role)
                .await().indefinitely();

        assertThat(roleId)
                .isNotNull()
                .isEqualTo(customRoleId);
    }

    @Test
    public void testGenerateSecretId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();
        assertThat(secretIdInfo.secretId)
                .isNotNull();
        assertThat(secretIdInfo.secretIdAccessor)
                .isNotNull();
        assertThat(secretIdInfo.secretIdNumUses)
                .isEqualTo(5);
        assertThat(secretIdInfo.secretIdTtl)
                .isEqualTo(60);
    }

    @Test
    public void testReadSecretId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl("1m")
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();
        assertThat(secretIdInfo.secretId)
                .isNotNull();
        assertThat(secretIdInfo.secretIdAccessor)
                .isNotNull();
        assertThat(secretIdInfo.secretIdNumUses)
                .isEqualTo(5);
        assertThat(secretIdInfo.secretIdTtl)
                .isEqualTo(60);

        // Read secret id
        var secretId = appRoleApi.readSecretId(role, secretIdInfo.secretId)
                .await().indefinitely();

        assertThat(secretId)
                .isNotNull();
        assertThat(secretId.cidrList)
                .isEmpty();
        assertThat(secretId.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretId.expirationTime)
                .isBetween(now().plusSeconds(59), now().plusSeconds(61));
        assertThat(secretId.lastUpdatedTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretId.metadata)
                .containsEntry("foo", "bar");
        assertThat(secretId.secretIdAccessor)
                .isNotNull();
        assertThat(secretId.secretIdNumUses)
                .isEqualTo(5);
        assertThat(secretId.secretIdTtl)
                .isEqualTo(60);
    }

    @Test
    public void testListSecretIdAccessors(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, null)
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();

        // List secret ids
        var accessors = appRoleApi.listSecretIdAccessors(role)
                .await().indefinitely();

        assertThat(accessors)
                .isNotNull()
                .contains(secretIdInfo.secretIdAccessor);
    }

    @Test
    public void testDestroySecretId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, null)
                .await().indefinitely();

        assertThat(secretIdInfo.secretId)
                .isNotNull();

        var secretIdData = appRoleApi.readSecretId(role, secretIdInfo.secretId)
                .await().indefinitely();

        assertThat(secretIdData)
                .isNotNull();

        // Destroy secret id
        appRoleApi.destroySecretId(role, secretIdInfo.secretId)
                .await().indefinitely();

        // Validate

        assertThatThrownBy(() -> appRoleApi.readSecretId(role, secretIdInfo.secretId).await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=204");
    }

    @Test
    public void testCreateCustomSecretId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null).await().indefinitely();

        var secretId = role + "-custom-secret-id";

        // Generate secret id
        var secretIdInfo = appRoleApi.createCustomSecretId(role, new VaultAuthAppRoleCreateCustomSecretIdParams()
                .setSecretId(secretId)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();
        assertThat(secretIdInfo.secretId)
                .isEqualTo(secretId);

        // Read secret id
        var secretIdData = appRoleApi.readSecretId(role, secretIdInfo.secretId)
                .await().indefinitely();

        assertThat(secretIdData)
                .isNotNull();
        assertThat(secretIdData.metadata)
                .containsEntry("foo", "bar");
    }

    @Test
    public void testReadSecretIdAccessor(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null).await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl("1m")
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();

        // Read secret id accessor
        var secretIdAccessor = appRoleApi.readSecretIdAccessor(role, secretIdInfo.secretIdAccessor)
                .await().indefinitely();

        assertThat(secretIdAccessor)
                .isNotNull();
        assertThat(secretIdAccessor.cidrList)
                .isEmpty();
        assertThat(secretIdAccessor.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretIdAccessor.expirationTime)
                .isBetween(now().plusSeconds(59), now().plusSeconds(61));
        assertThat(secretIdAccessor.lastUpdatedTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretIdAccessor.metadata)
                .containsEntry("foo", "bar");
        assertThat(secretIdAccessor.secretIdAccessor)
                .isNotNull();
        assertThat(secretIdAccessor.secretIdNumUses)
                .isEqualTo(5);
        assertThat(secretIdAccessor.secretIdTtl)
                .isEqualTo(60);
        assertThat(secretIdAccessor.tokenBoundCidrs)
                .isEmpty();
    }

    @Test
    public void testDestroySecretIdAccessor(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null).await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl("1m")
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();

        // Destroy secret id accessor
        appRoleApi.destroySecretIdAccessor(role, secretIdInfo.secretIdAccessor)
                .await().indefinitely();

        // Validate

        assertThatThrownBy(() -> appRoleApi.readSecretIdAccessor(role, secretIdInfo.secretIdAccessor).await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

    @Test
    public void testTidyTokens(VaultClient client) {
        var appRoleApi = client.auth().appRole();

        assertThatNoException()
                .isThrownBy(() -> appRoleApi.tidyTokens().await().indefinitely());
    }

}

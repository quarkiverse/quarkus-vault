package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleCreateCustomSecretIdParams;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdParams;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleUpdateRoleParams;
import io.quarkus.vault.client.api.common.VaultTokenType;
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
                .await().indefinitely().getSecretId();

        assertThat(secretId)
                .isNotNull();

        // Login
        var loginResult = appRoleApi.login(roleId, secretId)
                .await().indefinitely();

        assertThat(loginResult.getClientToken())
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
        assertThat(roleInfo.isBindSecretId())
                .isTrue();
        assertThat(roleInfo.getTokenBoundCidrs())
                .isEmpty();
        assertThat(roleInfo.getTokenExplicitMaxTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(roleInfo.getTokenMaxTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(roleInfo.isTokenNoDefaultPolicy())
                .isFalse();
        assertThat(roleInfo.getTokenNumUses())
                .isEqualTo(0);
        assertThat(roleInfo.getTokenPeriod())
                .isEqualTo(Duration.ZERO);
        assertThat(roleInfo.getTokenPolicies())
                .isEmpty();
        assertThat(roleInfo.getTokenType())
                .isEqualTo(VaultTokenType.DEFAULT);
    }

    @Test
    void testUpdateRole(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        var policyName = role + "-policy";

        client.sys().policy().update(policyName, """
                path "secret/data/%s" {
                  capabilities = ["create", "read", "update", "delete", "list"]
                }""".formatted(role))
                .await().indefinitely();

        // Create role
        appRoleApi.updateRole(role, new VaultAuthAppRoleUpdateRoleParams()
                .setBindSecretId(false)
                .setTokenBoundCidrs(List.of("127.0.0.1"))
                .setTokenExplicitMaxTtl(Duration.ofMinutes(5))
                .setTokenMaxTtl(Duration.ofMinutes(10))
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(5)
                .setTokenPeriod(Duration.ofMinutes(15))
                .setTokenPolicies(List.of(policyName))
                .setTokenType(VaultTokenType.SERVICE))
                .await().indefinitely();

        // Read role
        var roleInfo = appRoleApi.readRole(role)
                .await().indefinitely();

        assertThat(roleInfo)
                .isNotNull();
        assertThat(roleInfo.isBindSecretId())
                .isFalse();
        assertThat(roleInfo.getTokenBoundCidrs())
                .isEqualTo(List.of("127.0.0.1"));
        assertThat(roleInfo.getTokenExplicitMaxTtl())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(roleInfo.getTokenMaxTtl())
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(roleInfo.isTokenNoDefaultPolicy())
                .isTrue();
        assertThat(roleInfo.getTokenNumUses())
                .isEqualTo(5);
        assertThat(roleInfo.getTokenPeriod())
                .isEqualTo(Duration.ofMinutes(15));
        assertThat(roleInfo.getTokenPolicies())
                .isEqualTo(List.of(policyName));
        assertThat(roleInfo.getTokenType())
                .isEqualTo(VaultTokenType.SERVICE);
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

        // Read role id info
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
                .setTtl(Duration.ofMinutes(1))
                .setNumUses(5))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();
        assertThat(secretIdInfo.getSecretId())
                .isNotNull();
        assertThat(secretIdInfo.getSecretIdAccessor())
                .isNotNull();
        assertThat(secretIdInfo.getSecretIdNumUses())
                .isEqualTo(5);
        assertThat(secretIdInfo.getSecretIdTtl())
                .isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    public void testReadSecretId(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl(Duration.ofMinutes(1))
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();
        assertThat(secretIdInfo.getSecretId())
                .isNotNull();
        assertThat(secretIdInfo.getSecretIdAccessor())
                .isNotNull();
        assertThat(secretIdInfo.getSecretIdNumUses())
                .isEqualTo(5);
        assertThat(secretIdInfo.getSecretIdTtl())
                .isEqualTo(Duration.ofMinutes(1));

        // Read secret id
        var secretId = appRoleApi.readSecretId(role, secretIdInfo.getSecretId())
                .await().indefinitely();

        assertThat(secretId)
                .isNotNull();
        assertThat(secretId.getCidrList())
                .isEmpty();
        assertThat(secretId.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretId.getExpirationTime())
                .isBetween(now().plusSeconds(59), now().plusSeconds(61));
        assertThat(secretId.getLastUpdatedTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretId.getMetadata())
                .containsEntry("foo", "bar");
        assertThat(secretId.getSecretIdAccessor())
                .isNotNull();
        assertThat(secretId.getSecretIdNumUses())
                .isEqualTo(5);
        assertThat(secretId.getSecretIdTtl())
                .isEqualTo(Duration.ofMinutes(1));
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
                .contains(secretIdInfo.getSecretIdAccessor());
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

        assertThat(secretIdInfo.getSecretId())
                .isNotNull();

        var secretIdData = appRoleApi.readSecretId(role, secretIdInfo.getSecretId())
                .await().indefinitely();

        assertThat(secretIdData)
                .isNotNull();

        // Destroy secret id
        appRoleApi.destroySecretId(role, secretIdInfo.getSecretId())
                .await().indefinitely();

        // Validate

        assertThatThrownBy(() -> appRoleApi.readSecretId(role, secretIdInfo.getSecretId()).await().indefinitely())
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
        assertThat(secretIdInfo.getSecretId())
                .isEqualTo(secretId);

        // Read secret id
        var secretIdData = appRoleApi.readSecretId(role, secretIdInfo.getSecretId())
                .await().indefinitely();

        assertThat(secretIdData)
                .isNotNull();
        assertThat(secretIdData.getMetadata())
                .containsEntry("foo", "bar");
    }

    @Test
    public void testReadSecretIdAccessor(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null).await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl(Duration.ofMinutes(1))
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();

        // Read secret id accessor
        var secretIdAccessor = appRoleApi.readSecretIdAccessor(role, secretIdInfo.getSecretIdAccessor())
                .await().indefinitely();

        assertThat(secretIdAccessor)
                .isNotNull();
        assertThat(secretIdAccessor.getCidrList())
                .isEmpty();
        assertThat(secretIdAccessor.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretIdAccessor.getExpirationTime())
                .isBetween(now().plusSeconds(59), now().plusSeconds(61));
        assertThat(secretIdAccessor.getLastUpdatedTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(secretIdAccessor.getMetadata())
                .containsEntry("foo", "bar");
        assertThat(secretIdAccessor.getSecretIdAccessor())
                .isNotNull();
        assertThat(secretIdAccessor.getSecretIdNumUses())
                .isEqualTo(5);
        assertThat(secretIdAccessor.getSecretIdTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(secretIdAccessor.getTokenBoundCidrs())
                .isEmpty();
    }

    @Test
    public void testDestroySecretIdAccessor(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null).await().indefinitely();

        // Generate secret id
        var secretIdInfo = appRoleApi.generateSecretId(role, new VaultAuthAppRoleGenerateSecretIdParams()
                .setTtl(Duration.ofMinutes(1))
                .setNumUses(5)
                .setMetadata(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(secretIdInfo)
                .isNotNull();

        // Destroy secret id accessor
        appRoleApi.destroySecretIdAccessor(role, secretIdInfo.getSecretIdAccessor())
                .await().indefinitely();

        // Validate

        assertThatThrownBy(() -> appRoleApi.readSecretIdAccessor(role, secretIdInfo.getSecretIdAccessor())
                .await().indefinitely())
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

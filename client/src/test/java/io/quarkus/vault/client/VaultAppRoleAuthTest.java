package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "approle", path = "approle")
})
public class VaultAppRoleAuthTest {

    @Test
    public void testLoginProcess(VaultClient client, @Random String role) {
        var appRoleApi = client.auth().appRole();

        // Create role
        appRoleApi.updateRole(role, null)
                .await().indefinitely();

        // Read role id
        var roleId = appRoleApi.readRoleId(role)
                .await().indefinitely().roleId;
        assertThat(roleId).isNotNull();

        // Generate secret id
        var secretId = appRoleApi.generateSecretId(role, null)
                .await().indefinitely().secretId;
        assertThat(secretId).isNotNull();

        // Login
        var loginResult = appRoleApi.login(roleId, secretId)
                .await().indefinitely();
        assertThat(loginResult.clientToken).isNotNull();
    }

}

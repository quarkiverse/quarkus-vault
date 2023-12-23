package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPassUpdateUserParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "userpass", path = "userpass")
}, logLevel = "trace")
public class VaultAuthUserPassTest {

    @Test
    public void testLoginProcess(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();

        // Login
        var login = userPassApi.login(user, "test")
                .await().indefinitely();

        assertThat(login.clientToken)
                .isNotNull();
    }

    @Disabled("updateUserPolicies succeeds but doesnt apply policies, unknown if this is a bug in Vault or the client")
    @Test
    public void testUpdateUserPolicies(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read secret/* path
        client.sys().policy().update(userPolicy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();
        userPassApi.updateUserPolicies(user, List.of(userPolicy))
                .await().indefinitely();

        // Validate policies
        var userInfo = userPassApi.readUser(user)
                .await().indefinitely();

        assertThat(userInfo.tokenPolicies)
                .containsExactly(userPolicy);
    }

    @Test
    public void testUpdateUserPoliciesViaRegularUpdate(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read secret/* path
        client.sys().policy().update(userPolicy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenMaxTtl("15h"))
                .await().indefinitely();

        // Update user

        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setTokenPolicies(List.of(userPolicy)))
                .await().indefinitely();

        // Validate policies (and verify that token max ttl is still set)
        var userInfo = userPassApi.readUser(user)
                .await().indefinitely();

        assertThat(userInfo.tokenPolicies)
                .contains(userPolicy);
        assertThat(userInfo.tokenMaxTtl)
                .isEqualTo("54000");
    }

    @Test
    public void testUpdateUserPassword(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();

        // Validate via login

        var login = userPassApi.login(user, "test")
                .await().indefinitely();

        assertThat(login.clientToken)
                .isNotNull();

        // Update password

        userPassApi.updateUserPassword(user, "test2")
                .await().indefinitely();

        // Validate via login

        var login2 = userPassApi.login(user, "test2")
                .await().indefinitely();

        assertThat(login2.clientToken)
                .isNotNull();
    }

    @Test
    public void testListUsers(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        var user1 = user.toLowerCase() + "1";
        var user2 = user.toLowerCase() + "2";

        // Create users
        userPassApi.updateUser(user1, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();
        userPassApi.updateUser(user2, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();

        // List users
        var users = userPassApi.listUsers()
                .await().indefinitely();

        assertThat(users)
                .contains(user1, user2);
    }

    @Test
    public void testDeleteUser(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        var user1 = user.toLowerCase() + "1";
        var user2 = user.toLowerCase() + "2";

        // Create users
        userPassApi.updateUser(user1, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();
        userPassApi.updateUser(user2, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .await().indefinitely();

        // List users
        var users = userPassApi.listUsers()
                .await().indefinitely();

        assertThat(users)
                .contains(user1, user2);

        // Delete user
        userPassApi.deleteUser(user1)
                .await().indefinitely();

        // Validate

        var users2 = userPassApi.listUsers()
                .await().indefinitely();

        assertThat(users2)
                .contains(user2);
    }

    @Test
    public void testClientLogin(VaultClient client, @Random String user) {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read sys/mounts/* path
        client.sys().policy().update(userPolicy, """
                path "sys/mounts/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        // Create user with policy
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenPolicies(List.of(userPolicy))
                .setTokenNoDefaultPolicy(true))
                .await().indefinitely();

        // Login
        var authClient = client.configure()
                .userPass(user, "test")
                .build();

        // Validate

        assertThatThrownBy(() -> authClient.sys().auth().read("token").await().indefinitely())
                .isInstanceOf(VaultException.class)
                .asString().contains("permission denied");

        var mountInfo = authClient.sys().mounts().read("secret")
                .await().indefinitely();

        assertThat(mountInfo.description)
                .isNotNull();
    }

}

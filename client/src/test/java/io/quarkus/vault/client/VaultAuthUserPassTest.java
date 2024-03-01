package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPassUpdateUserParams;
import io.quarkus.vault.client.api.common.VaultTokenType;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "userpass", path = "userpass")
}, logLevel = "trace")
public class VaultAuthUserPassTest {

    @Test
    public void testLoginProcess(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();

        // Login
        var login = userPassApi.login(user, "test")
                .toCompletableFuture().get();

        assertThat(login.getClientToken())
                .isNotNull();
    }

    @Test
    public void testUpdateUser(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setTokenBoundCidrs(List.of("127.0.0.1"))
                .setTokenTtl(Duration.ofHours(1))
                .setTokenMaxTtl(Duration.ofHours(2))
                .setTokenPolicies(List.of("default"))
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(5)
                .setTokenPeriod(Duration.ofMinutes(10))
                .setTokenExplicitMaxTtl(Duration.ofHours(3))
                .setPassword("test")
                .setTokenType(VaultTokenType.DEFAULT))
                .toCompletableFuture().get();

        var userInfo = userPassApi.readUser(user)
                .toCompletableFuture().get();

        assertThat(userInfo.getTokenBoundCidrs())
                .contains("127.0.0.1");
        assertThat(userInfo.getTokenTtl())
                .isEqualTo(Duration.ofHours(1));
        assertThat(userInfo.getTokenMaxTtl())
                .isEqualTo(Duration.ofHours(2));
        assertThat(userInfo.getTokenPolicies())
                .contains("default");
        assertThat(userInfo.isTokenNoDefaultPolicy())
                .isTrue();
        assertThat(userInfo.getTokenNumUses())
                .isEqualTo(5);
        assertThat(userInfo.getTokenPeriod())
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(userInfo.getTokenExplicitMaxTtl())
                .isEqualTo(Duration.ofHours(3));
        assertThat(userInfo.getTokenType())
                .isEqualTo(VaultTokenType.DEFAULT);
    }

    @Disabled("updateUserPolicies succeeds but doesnt apply policies, unknown if this is a bug in Vault or the client")
    @Test
    public void testUpdateUserPolicies(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read secret/* path
        client.sys().policy().update(userPolicy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .toCompletableFuture().get();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();
        userPassApi.updateUserPolicies(user, List.of(userPolicy))
                .toCompletableFuture().get();

        // Validate policies
        var userInfo = userPassApi.readUser(user)
                .toCompletableFuture().get();

        assertThat(userInfo.getTokenPolicies())
                .containsExactly(userPolicy);
    }

    @Test
    public void testUpdateUserPoliciesViaRegularUpdate(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read secret/* path
        client.sys().policy().update(userPolicy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .toCompletableFuture().get();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenMaxTtl(Duration.ofHours(15)))
                .toCompletableFuture().get();

        // Update user

        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setTokenPolicies(List.of(userPolicy)))
                .toCompletableFuture().get();

        // Validate policies (and verify that token max ttl is still set)
        var userInfo = userPassApi.readUser(user)
                .toCompletableFuture().get();

        assertThat(userInfo.getTokenPolicies())
                .contains(userPolicy);
        assertThat(userInfo.getTokenMaxTtl())
                .isEqualTo(Duration.ofHours(15));
    }

    @Test
    public void testUpdateUserPassword(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        // Create user
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();

        // Validate via login

        var login = userPassApi.login(user, "test")
                .toCompletableFuture().get();

        assertThat(login.getClientToken())
                .isNotNull();

        // Update password

        userPassApi.updateUserPassword(user, "test2")
                .toCompletableFuture().get();

        // Validate via login

        var login2 = userPassApi.login(user, "test2")
                .toCompletableFuture().get();

        assertThat(login2.getClientToken())
                .isNotNull();
    }

    @Test
    public void testListUsers(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        var user1 = user.toLowerCase() + "1";
        var user2 = user.toLowerCase() + "2";

        // Create users
        userPassApi.updateUser(user1, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();
        userPassApi.updateUser(user2, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();

        // List users
        var users = userPassApi.listUsers()
                .toCompletableFuture().get();

        assertThat(users)
                .contains(user1, user2);
    }

    @Test
    public void testDeleteUser(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        var user1 = user.toLowerCase() + "1";
        var user2 = user.toLowerCase() + "2";

        // Create users
        userPassApi.updateUser(user1, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();
        userPassApi.updateUser(user2, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test"))
                .toCompletableFuture().get();

        // List users
        var users = userPassApi.listUsers()
                .toCompletableFuture().get();

        assertThat(users)
                .contains(user1, user2);

        // Delete user
        userPassApi.deleteUser(user1)
                .toCompletableFuture().get();

        // Validate

        var users2 = userPassApi.listUsers()
                .toCompletableFuture().get();

        assertThat(users2)
                .contains(user2);
    }

    @Test
    public void testClientLogin(VaultClient client, @Random String user) throws Exception {
        var userPassApi = client.auth().userPass();

        var userPolicy = user + "-policy";

        // Add policy to read sys/mounts/* path
        client.sys().policy().update(userPolicy, """
                path "sys/mounts/*" {
                    capabilities = [ "read" ]
                }""")
                .toCompletableFuture().get();

        // Create user with policy
        userPassApi.updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenPolicies(List.of(userPolicy))
                .setTokenNoDefaultPolicy(true))
                .toCompletableFuture().get();

        // Login
        var authClient = client.configure()
                .userPass(user, "test")
                .build();

        // Validate

        assertThatThrownBy(() -> authClient.sys().auth().read("token").toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasMessageContaining("permission denied");

        var mountInfo = authClient.sys().mounts().read("secret")
                .toCompletableFuture().get();

        assertThat(mountInfo.getDescription())
                .isNotNull();
    }

}

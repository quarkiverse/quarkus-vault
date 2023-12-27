package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleUpdateRoleParams;
import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPassUpdateUserParams;
import io.quarkus.vault.client.auth.VaultAppRoleAuthOptions;
import io.quarkus.vault.client.auth.VaultStaticClientTokenAuthOptions;
import io.quarkus.vault.client.auth.VaultUserPassAuthOptions;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest(auths = {
        @VaultClientTest.Mount(type = "approle", path = "approle"),
        @VaultClientTest.Mount(type = "userpass", path = "userpass")
})
public class VaultUnwrappingValueProviderTest {

    @Test
    public void testTokenCreateUnwrap(VaultClient client) {

        var wrapped = client.auth().token().wrapping(Duration.ofSeconds(10), (api) -> api.create(null))
                .await().indefinitely();

        var unwrappingClient = client.configure().clientToken(VaultStaticClientTokenAuthOptions.builder()
                .unwrappingToken(wrapped.getToken()).build())
                .build();

        assertThatCode(() -> unwrappingClient.secrets().kv2().listSecrets("/")
                .await().indefinitely())
                .doesNotThrowAnyException();
    }

    @Test
    public void testSecretIdUnwrap(VaultClient client, @Random String role) {

        var policyName = role + "-policy";

        client.sys().policy().update(policyName, """
                path "secret/*" { capabilities = ["create", "read", "update", "delete", "list"] }
                """)
                .await().indefinitely();

        client.auth().appRole().updateRole(role, new VaultAuthAppRoleUpdateRoleParams()
                .setTokenPolicies(List.of(policyName)))
                .await().indefinitely();

        var roleId = client.auth().appRole().readRoleId(role)
                .await().indefinitely();

        var wrapped = client.auth().appRole()
                .wrapping(Duration.ofSeconds(10), (api) -> api.generateSecretId("approle", role, null))
                .await().indefinitely();

        var unwrappingClient = client.configure()
                .appRole(VaultAppRoleAuthOptions.builder()
                        .roleId(roleId)
                        .unwrappingSecretId(wrapped.getToken())
                        .build())
                .build();

        assertThatCode(() -> unwrappingClient.secrets().kv2().listSecrets("/")
                .await().indefinitely())
                .doesNotThrowAnyException();
    }

    @Test
    public void testPasswordUnwrap(VaultClient client, @Random String user, @Random String password) {

        var policyName = user + "-policy";

        client.sys().policy().update(policyName, """
                path "secret/*" { capabilities = ["create", "read", "update", "delete", "list"] }
                """)
                .await().indefinitely();

        client.auth().userPass().updateUser(user, new VaultAuthUserPassUpdateUserParams()
                .setTokenPolicies(List.of(policyName))
                .setPassword(password))
                .await().indefinitely();

        client.secrets().kv2().updateSecret("login", null, Map.of("password", password))
                .await().indefinitely();

        var wrapped = client.secrets().kv2().wrapping(Duration.ofSeconds(10),
                (api) -> api.readSecret("secret", "login"))
                .await().indefinitely();

        var unwrappingClient = client.configure().userPass(VaultUserPassAuthOptions.builder()
                .username(user)
                .unwrappingPassword(wrapped.getToken(), 2)
                .build())
                .build();

        assertThatCode(() -> unwrappingClient.secrets().kv2().listSecrets("/")
                .await().indefinitely())
                .doesNotThrowAnyException();
    }

}

package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysPoliciesTest {

    @Test
    public void testList(VaultClient client) {
        var policyApi = client.sys().policy();

        var policies = policyApi.list()
                .await().indefinitely();

        assertThat(policies)
                .contains("root", "default");
    }

    @Test
    public void testRead(VaultClient client) {
        var policyApi = client.sys().policy();

        var policy = policyApi.read("default")
                .await().indefinitely();

        assertThat(policy)
                .isNotNull();
        assertThat(policy.name)
                .isEqualTo("default");
        assertThat(policy.rules)
                .contains("""
                        path "auth/token/lookup-self" {
                            capabilities = ["read"]
                        }
                        """);
    }

    @Test
    public void testUpdate(VaultClient client, @Random String policy) {
        var policyApi = client.sys().policy();

        var rules = """
                path "auth/token/lookup-self" {
                    capabilities = ["read"]
                }
                """;

        policyApi.update(policy, rules)
                .await().indefinitely();

        assertThat(policyApi.read(policy)
                .await().indefinitely().rules)
                .isEqualTo(rules);
    }

    @Test
    public void testDelete(VaultClient client, @Random String policy) {
        var policyApi = client.sys().policy();

        var rules = """
                path "auth/token/lookup-self" {
                    capabilities = ["read"]
                }
                """;

        policyApi.update(policy, rules)
                .await().indefinitely();

        assertThat(policyApi.list().await().indefinitely())
                .contains(policy);

        policyApi.delete(policy)
                .await().indefinitely();

        assertThat(policyApi.list().await().indefinitely())
                .doesNotContain(policy);
    }
}

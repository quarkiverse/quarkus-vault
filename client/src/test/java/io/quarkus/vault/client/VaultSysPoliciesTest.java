package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysPoliciesTest {

    @Test
    public void testList(VaultClient client) throws Exception {
        var policyApi = client.sys().policy();

        var policies = policyApi.list()
                .toCompletableFuture().get();

        assertThat(policies)
                .contains("root", "default");
    }

    @Test
    public void testRead(VaultClient client) throws Exception {
        var policyApi = client.sys().policy();

        var policy = policyApi.read("default")
                .toCompletableFuture().get();

        assertThat(policy)
                .isNotNull();
        assertThat(policy.getName())
                .isEqualTo("default");
        assertThat(policy.getRules())
                .contains("""
                        path "auth/token/lookup-self" {
                            capabilities = ["read"]
                        }
                        """);
    }

    @Test
    public void testUpdate(VaultClient client, @Random String policy) throws Exception {
        var policyApi = client.sys().policy();

        var rules = """
                path "auth/token/lookup-self" {
                    capabilities = ["read"]
                }
                """;

        policyApi.update(policy, rules)
                .toCompletableFuture().get();

        assertThat(policyApi.read(policy)
                .toCompletableFuture().get()
                .getRules())
                .isEqualTo(rules);
    }

    @Test
    public void testDelete(VaultClient client, @Random String policy) throws Exception {
        var policyApi = client.sys().policy();

        var rules = """
                path "auth/token/lookup-self" {
                    capabilities = ["read"]
                }
                """;

        policyApi.update(policy, rules)
                .toCompletableFuture().get();

        assertThat(policyApi.list().toCompletableFuture().get())
                .contains(policy);

        policyApi.delete(policy)
                .toCompletableFuture().get();

        assertThat(policyApi.list().toCompletableFuture().get())
                .doesNotContain(policy);
    }
}

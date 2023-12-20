package io.quarkus.vault.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;
import io.quarkus.vault.client.util.JsonMapping;

public class JDKVaultClientTest {

    static VaultContainer<?> vault = new VaultContainer<>("hashicorp/vault:1.15.2")
            .withVaultToken("root")
            .withInitCommand("kv put secret/hello value=world");

    @BeforeAll
    static void startVault() {
        vault.start();
    }

    @AfterAll
    static void stopVault() {
        vault.stop();
    }

    @Test
    void testExample() throws Exception {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            // Start Vault client unauthenticated and with tracing enabled

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(vault.getHttpHostAddress())
                    .traceRequests()
                    .build();

            // Unauthenticated request to health endpoint

            var healthApi = client.sys().health();

            var statusResult = healthApi.status()
                    .await().indefinitely();
            assertEquals(statusResult, 200);

            var infoResult = healthApi.info()
                    .await().indefinitely();

            System.out.println(JsonMapping.mapper.writeValueAsString(infoResult));

            // Try reading a secret without authentication

            assertThrows(VaultClientException.class, () -> client.secrets().kv2().readSecret("hello")
                    .await().indefinitely());

            // Switch to root token authentication and try reading a secret

            client.configure()
                    .clientToken("root")
                    .build()
                    .secrets().kv2().readSecret("hello")
                    .await().indefinitely();
        }
    }

    @Test
    void testKV1() throws Exception {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(vault.getHttpHostAddress())
                    .clientToken("root")
                    .build();

            var kvApi = client.secrets().kv2();

            var readResult = kvApi.readSecret("hello")
                    .await().indefinitely();
            assertEquals(readResult.getData().get("value"), "world");
        }
    }

}

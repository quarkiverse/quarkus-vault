package io.quarkus.vault.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.http.HttpClient;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;
import io.quarkus.vault.client.json.JsonMapping;
import io.quarkus.vault.client.test.Vault;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultClientExampleTest {

    @Disabled("Example for client usage, replicated in other tests and tracing generates a lot of noise")
    @Test
    void testExample(@Vault URL baseURL) throws Exception {

        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            // Start Vault client unauthenticated and with tracing enabled

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(baseURL)
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

    @Disabled("Example for client usage, replicated in other tests")
    @Test
    void testKV1(@Vault URL baseURL) {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(baseURL)
                    .clientToken("root")
                    .build();

            var kvApi = client.secrets().kv2();

            var secret = kvApi.readSecret("hello")
                    .await().indefinitely();
            assertEquals(secret.getData().get("value"), "world");
        }
    }

}

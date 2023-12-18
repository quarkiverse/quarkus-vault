package io.quarkus.vault.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URL;
import java.net.http.HttpClient;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretData;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretResult;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;
import io.quarkus.vault.client.util.JsonMapping;
import io.smallrye.mutiny.Uni;

public class JDKVaultClientTest {

    @Test
    void testHealth() throws Exception {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(new URL("http://localhost:8200"))
                    .traceRequests()
                    .build();

            var healthApi = client.sys().health();

            var statusResult = healthApi.status()
                    .await().indefinitely();
            assertEquals(statusResult.getStatusCode(), 200);

            var infoResult = healthApi.info()
                    .await().indefinitely();
            System.out.println(JsonMapping.mapper.writeValueAsString(infoResult));

            assertThrows(VaultClientException.class, () -> client.secrets().kv2().readSecret("hello")
                    .await().indefinitely());

            // Switch to root token authentication and try reading a secret
            client.configure().clientToken("root").build()
                    .secrets().kv2().readSecret("hello")
                    .await().indefinitely();
        }
    }

    @Test
    void testKV1() throws Exception {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .executor(httpClient)
                    .baseUrl(new URL("http://localhost:8200"))
                    .clientToken("root")
                    .build();

            var kvApi = client.secrets().kv2();

            var readResult = kvApi.readSecret("hello")
                    .await().indefinitely();
            assertEquals(readResult.getData().get("value"), "world");
        }
    }

    @Test
    void testMockExecution() throws Exception {
        var client = VaultClient.builder()
                .baseUrl(new URL("http://example.com"))
                .executor(new VaultRequestExecutor() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public <T> Uni<T> execute(VaultRequest<T> request) {
                        System.out.println("Returning mock for request: " + request.getOperation());
                        return Uni.createFrom().item((T) new VaultSecretsKV2ReadSecretResult()
                                .setData(new VaultSecretsKV2ReadSecretData()
                                        .setData(Map.of("value", "world"))));
                    }
                })
                .build();

        var kvApi = client.secrets().kv2();

        var readResult = kvApi.readSecret("hello")
                .await().indefinitely();
        assertEquals(readResult.getData().get("value"), "world");
    }

}

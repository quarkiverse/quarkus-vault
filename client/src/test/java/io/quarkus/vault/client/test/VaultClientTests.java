package io.quarkus.vault.client.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretData;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretResult;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.smallrye.mutiny.Uni;

public class VaultClientTests {

    /**
     * Example of easy request mocking using a custom executor.
     */
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

package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.api.sys.init.VaultSysInitParams;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;

@Testcontainers
public class VaultSysSealTest {

    VaultContainer<?> container = new VaultContainer<>("hashicorp/vault:1.15.2")
            .withEnv("VAULT_LOG_LEVEL", "debug")
            .withClasspathResourceMapping("simple-config.hcl", "/vault/config/vault.hcl", BindMode.READ_ONLY)
            .withCommand("server");

    @BeforeEach
    public void init() {
        container.setWaitStrategy(Wait.forHttp("/v1/sys/health").forStatusCode(501));
        container.start();
    }

    @AfterEach
    public void teardown() {
        container.stop();
    }

    @Test
    public void testUnseal() {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .baseUrl("http://" + container.getHost() + ":" + container.getMappedPort(8200))
                    .executor(httpClient)
                    .build();

            var initApi = client.sys().init();

            var info = initApi.status()
                    .await().indefinitely();

            assertThat(info.isInitialized())
                    .isFalse();

            var init = initApi.init(new VaultSysInitParams()
                    .setSecretThreshold(3)
                    .setSecretShares(5))
                    .await().indefinitely();

            assertThat(init.getKeys())
                    .hasSize(5);
            assertThat(init.getKeysBase64())
                    .hasSize(5);
            assertThat(init.getRootToken())
                    .isNotEmpty();

            var sealApi = client.sys().seal();

            var unseal1 = sealApi.unseal(init.getKeys().get(0), false, false)
                    .await().indefinitely();
            assertThat(unseal1.isSealed())
                    .isTrue();
            assertThat(unseal1.getProgress())
                    .isEqualTo(1);

            var unseal2 = sealApi.unseal(init.getKeys().get(2), false, false)
                    .await().indefinitely();
            assertThat(unseal2.isSealed())
                    .isTrue();
            assertThat(unseal2.getProgress())
                    .isEqualTo(2);

            var unseal3 = sealApi.unseal(init.getKeys().get(4), false, false)
                    .await().indefinitely();
            assertThat(unseal3.isSealed())
                    .isFalse();
            assertThat(unseal3.getProgress())
                    .isEqualTo(0);
        }
    }

    @Test
    public void testSeal() {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .baseUrl("http://" + container.getHost() + ":" + container.getMappedPort(8200))
                    .executor(httpClient)
                    .build();

            // Init

            var initApi = client.sys().init();

            var info = initApi.status()
                    .await().indefinitely();

            assertThat(info.isInitialized())
                    .isFalse();

            var init = initApi.init(new VaultSysInitParams()
                    .setSecretThreshold(2)
                    .setSecretShares(3))
                    .await().indefinitely();

            assertThat(init.getKeys())
                    .hasSize(3);

            // Unseal

            var sealApi = client.sys().seal();

            var unseal1 = sealApi.unseal(init.getKeys().get(0), false, false)
                    .await().indefinitely();
            assertThat(unseal1.isSealed())
                    .isTrue();

            var unseal2 = sealApi.unseal(init.getKeys().get(1), false, false)
                    .await().indefinitely();
            assertThat(unseal2.isSealed())
                    .isFalse();

            // Seal

            sealApi = client.configure().clientToken(init.getRootToken()).build().sys().seal();

            sealApi.seal()
                    .await().indefinitely();

            var status = sealApi.status()
                    .await().indefinitely();
            assertThat(status.isSealed())
                    .isTrue();
        }
    }

    @Test
    public void testBackendStatus() {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .baseUrl("http://" + container.getHost() + ":" + container.getMappedPort(8200))
                    .executor(httpClient)
                    .build();

            var initApi = client.sys().init();

            var info = initApi.status()
                    .await().indefinitely();

            assertThat(info.isInitialized())
                    .isFalse();

            var init = initApi.init(new VaultSysInitParams()
                    .setSecretThreshold(3)
                    .setSecretShares(5))
                    .await().indefinitely();

            assertThat(init.getKeys())
                    .hasSize(5);
            assertThat(init.getKeysBase64())
                    .hasSize(5);
            assertThat(init.getRootToken())
                    .isNotEmpty();

            var sealApi = client.sys().seal();

            var status = sealApi.backendStatus()
                    .await().indefinitely();

            assertThat(status.isHealthy())
                    .isTrue();

            assertThat(status.getBackends())
                    .hasSize(1);

            var backendStatus = status.getBackends().get(0);

            assertThat(backendStatus.getName())
                    .isNotNull();
            assertThat(backendStatus.isHealthy())
                    .isTrue();
            assertThat(backendStatus.getUnhealthySince())
                    .isNull();
        }
    }

}

package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.http.HttpClient;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.api.sys.init.VaultSysInitParams;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;

public class VaultSysInitTest {

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
    public void testInit() {
        try (var httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient())) {

            var client = VaultClient.builder()
                    .baseUrl("http://" + container.getHost() + ":" + container.getFirstMappedPort())
                    .executor(httpClient)
                    .build();

            var info = client.sys().init().status()
                    .await().indefinitely();

            assertThat(info.isInitialized())
                    .isFalse();

            var init = client.sys().init().init(new VaultSysInitParams()
                    .setSecretThreshold(3)
                    .setSecretShares(9))
                    .await().indefinitely();

            assertThat(init.getKeys())
                    .hasSize(9);
            assertThat(init.getKeysBase64())
                    .hasSize(9);
            assertThat(init.getRootToken())
                    .isNotEmpty();
        }
    }
}

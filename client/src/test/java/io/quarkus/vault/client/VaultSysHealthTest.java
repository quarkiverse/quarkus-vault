package io.quarkus.vault.client;

import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysHealthTest {

    @Test
    public void testStatus(VaultClient client) {
        var healthApi = client.sys().health();

        var status = healthApi.status()
                .await().indefinitely();

        assertThat(status)
                .isEqualTo(200);
    }

    @Test
    public void testInfo(VaultClient client) {
        var healthApi = client.sys().health();

        var health = healthApi.info()
                .await().indefinitely();

        assertThat(health.isInitialized)
                .isTrue();
        assertThat(health.isStandby)
                .isFalse();
        assertThat(health.isSealed)
                .isFalse();
        assertThat(health.performanceStandby)
                .isFalse();
        assertThat(health.replicationPerformanceMode)
                .isEqualTo("disabled");
        assertThat(health.replicationDrMode)
                .isEqualTo("disabled");
        assertThat(health.serverTimeUtc)
                .isBetween(now().minusSeconds(1).getEpochSecond(), now().plusSeconds(1).getEpochSecond());
        assertThat(health.version)
                .startsWith("1.");
        assertThat(health.clusterName)
                .startsWith("vault-cluster-");
        assertThat(health.clusterId)
                .isNotNull();
    }

    @Test
    public void testInfoWithOptions(VaultClient client) {

        var healthApi = client.sys().health();

        var health = healthApi.info(true, false)
                .await().indefinitely();

        assertThat(health.isInitialized)
                .isTrue();
    }

}

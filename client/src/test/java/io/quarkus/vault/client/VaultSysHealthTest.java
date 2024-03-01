package io.quarkus.vault.client;

import static io.quarkus.vault.client.api.sys.health.VaultHealthStatus.INITIALIZED_UNSEALED_ACTIVE;
import static java.time.Instant.now;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysHealthTest {

    @Test
    public void testStatus(VaultClient client) throws Exception {
        var healthApi = client.sys().health();

        var status = healthApi.status()
                .toCompletableFuture().get();

        assertThat(status)
                .isEqualTo(INITIALIZED_UNSEALED_ACTIVE);
    }

    @Test
    public void testInfo(VaultClient client) throws Exception {
        var healthApi = client.sys().health();

        var health = healthApi.info()
                .toCompletableFuture().get();

        assertThat(health.isInitialized())
                .isTrue();
        assertThat(health.isStandby())
                .isFalse();
        assertThat(health.isSealed())
                .isFalse();
        assertThat(health.isPerformanceStandby())
                .isFalse();
        assertThat(health.getReplicationPerformanceMode())
                .isEqualTo("disabled");
        assertThat(health.getReplicationDrMode())
                .isEqualTo("disabled");
        assertThat(health.getServerTimeUtc())
                .isBetween(now().minusSeconds(1).getEpochSecond(), now().plusSeconds(1).getEpochSecond());
        assertThat(health.getVersion())
                .startsWith("1.");
        assertThat(health.getClusterName())
                .startsWith("vault-cluster-");
        assertThat(health.getClusterId())
                .isNotNull();
    }

    @Test
    public void testInfoWithOptions(VaultClient client) throws Exception {

        var healthApi = client.sys().health();

        var health = healthApi.info(true, false)
                .toCompletableFuture().get();

        assertThat(health.isInitialized())
                .isTrue();
    }

}

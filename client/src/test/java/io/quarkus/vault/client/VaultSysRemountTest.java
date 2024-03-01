package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysRemountTest {

    @Test
    public void testRemountAndStatus(VaultClient client, @Random String path, @Random String newPath) throws Exception {
        var remountApi = client.sys().remount();

        client.sys().mounts().enable(path, "kv", null, null, null)
                .toCompletableFuture().get();

        var remount = remountApi.remount(path, newPath)
                .toCompletableFuture().get();

        assertThat(remount)
                .isNotNull();
        assertThat(remount.getMigrationId())
                .isNotEmpty();

        var status = remountApi.status(remount.getMigrationId())
                .toCompletableFuture().get();

        assertThat(status)
                .isNotNull();
        assertThat(status.getMigrationInfo())
                .isNotNull();
        assertThat(status.getMigrationInfo().getStatus())
                .isEqualTo("success");
        assertThat(status.getMigrationInfo().getSourceMount())
                .isEqualTo(path + "/");
        assertThat(status.getMigrationInfo().getTargetMount())
                .isEqualTo(newPath + "/");
    }

}

package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysRemountTest {

    @Test
    public void testRemountAndStatus(VaultClient client, @Random String path, @Random String newPath) {
        var remountApi = client.sys().remount();

        client.sys().mounts().enable(path, "kv", null, null)
                .await().indefinitely();

        var remount = remountApi.remount(path, newPath)
                .await().indefinitely();

        assertThat(remount)
                .isNotNull();
        assertThat(remount.migrationId)
                .isNotEmpty();

        var status = remountApi.status(remount.migrationId)
                .await().indefinitely();

        assertThat(status)
                .isNotNull();
        assertThat(status.migrationInfo)
                .isNotNull();
        assertThat(status.migrationInfo.status)
                .isEqualTo("success");
        assertThat(status.migrationInfo.sourceMount)
                .isEqualTo(path + "/");
        assertThat(status.migrationInfo.targetMount)
                .isEqualTo(newPath + "/");
    }

}

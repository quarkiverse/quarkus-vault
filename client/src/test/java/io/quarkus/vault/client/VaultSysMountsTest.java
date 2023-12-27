package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsTuneOptions;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysMountsTest {

    @Test
    public void testList(VaultClient client) {
        var mountApi = client.sys().mounts();

        var mounts = mountApi.list()
                .await().indefinitely();

        assertThat(mounts)
                .isNotNull()
                .containsKey("secret/");

        var kvMountInfo = mounts.get("secret/");

        assertThat(kvMountInfo.getAccessor())
                .startsWith("kv_");
        assertThat(kvMountInfo.getConfig())
                .isNotNull();
        assertThat(kvMountInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(kvMountInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(kvMountInfo.getConfig().isForceNoCache())
                .isFalse();
        assertThat(kvMountInfo.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(kvMountInfo.getDescription())
                .isEqualTo("key/value secret storage");
        assertThat(kvMountInfo.isExternalEntropyAccess())
                .isFalse();
        assertThat(kvMountInfo.isLocal())
                .isFalse();
        assertThat(kvMountInfo.getOptions())
                .isNotNull()
                .containsEntry("version", "2");
        assertThat(kvMountInfo.getPluginVersion())
                .isEmpty();
        assertThat(kvMountInfo.getRunningPluginVersion())
                .startsWith("v")
                .endsWith("+builtin");
        assertThat(kvMountInfo.getRunningSha256())
                .isEmpty();
        assertThat(kvMountInfo.isSealWrap())
                .isFalse();
        assertThat(kvMountInfo.getType())
                .isEqualTo("kv");
        assertThat(kvMountInfo.getUuid())
                .isNotEmpty();
    }

    @Test
    public void testRead(VaultClient client) {
        var mountApi = client.sys().mounts();

        var kvMountInfo = mountApi.read("secret/")
                .await().indefinitely();

        assertThat(kvMountInfo.getAccessor())
                .startsWith("kv_");
        assertThat(kvMountInfo.getConfig())
                .isNotNull();
        assertThat(kvMountInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(kvMountInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(Duration.ZERO);
        assertThat(kvMountInfo.getConfig().isForceNoCache())
                .isFalse();
        assertThat(kvMountInfo.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(kvMountInfo.getDescription())
                .isEqualTo("key/value secret storage");
        assertThat(kvMountInfo.isExternalEntropyAccess())
                .isFalse();
        assertThat(kvMountInfo.isLocal())
                .isFalse();
        assertThat(kvMountInfo.getOptions())
                .isNotNull()
                .containsEntry("version", "2");
        assertThat(kvMountInfo.getPluginVersion())
                .isEmpty();
        assertThat(kvMountInfo.getRunningPluginVersion())
                .startsWith("v")
                .endsWith("+builtin");
        assertThat(kvMountInfo.getRunningSha256())
                .isEmpty();
        assertThat(kvMountInfo.isSealWrap())
                .isFalse();
        assertThat(kvMountInfo.getType())
                .isEqualTo("kv");
        assertThat(kvMountInfo.getUuid())
                .isNotEmpty();
    }

    @Test
    public void testEnable(VaultClient client, @Random String path) {
        var mountApi = client.sys().mounts();

        var mountPath = path + "/";

        mountApi.enable(path, "kv", null, null)
                .await().indefinitely();

        var mounts = mountApi.list()
                .await().indefinitely();

        assertThat(mounts)
                .containsKey(mountPath);
    }

    @Test
    public void testDisable(VaultClient client, @Random String path) {
        var mountApi = client.sys().mounts();

        var mountPath = path + "/";

        mountApi.enable(path, "kv", null, null)
                .await().indefinitely();

        var mounts = mountApi.list()
                .await().indefinitely();

        assertThat(mounts)
                .containsKey(mountPath);

        mountApi.disable(path)
                .await().indefinitely();

        mounts = mountApi.list()
                .await().indefinitely();

        assertThat(mounts)
                .doesNotContainKey(mountPath);
    }

    @Test
    public void testReadTune(VaultClient client) {
        var mountApi = client.sys().mounts();

        var kvTuneInfo = mountApi.readTune("secret/")
                .await().indefinitely();

        assertThat(kvTuneInfo.getDescription())
                .isEqualTo("key/value secret storage");
        assertThat(kvTuneInfo.getDefaultLeaseTtl())
                .isEqualTo(Duration.ofDays(32));
        assertThat(kvTuneInfo.getMaxLeaseTtl())
                .isEqualTo(Duration.ofDays(32));
        assertThat(kvTuneInfo.isForceNoCache())
                .isFalse();
        assertThat(kvTuneInfo.getAllowedManagedKeys())
                .isNull();
        assertThat(kvTuneInfo.getAuditNonHmacRequestKeys())
                .isNull();
        assertThat(kvTuneInfo.getAuditNonHmacResponseKeys())
                .isNull();
        assertThat(kvTuneInfo.getListingVisibility())
                .isNull();
        assertThat(kvTuneInfo.getPassthroughRequestHeaders())
                .isNull();
        assertThat(kvTuneInfo.getAllowedResponseHeaders())
                .isNull();
    }

    @Test
    public void testTune(VaultClient client, @Random String path) {
        var mountApi = client.sys().mounts();

        mountApi.enable(path, "kv", null, null)
                .await().indefinitely();

        mountApi.tune(path, new VaultSysMountsTuneOptions()
                .setDescription("test mount")
                .setDefaultLeaseTtl(Duration.ofSeconds(90))
                .setMaxLeaseTtl(Duration.ofMinutes(2))
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility("hidden")
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setAllowedManagedKeys(List.of("key5", "key6")))
                .await().indefinitely();

        var kvTuneInfo = mountApi.readTune(path)
                .await().indefinitely();

        assertThat(kvTuneInfo.getDefaultLeaseTtl())
                .isEqualTo(Duration.ofSeconds(90));
        assertThat(kvTuneInfo.getMaxLeaseTtl())
                .isEqualTo(Duration.ofMinutes(2));
        assertThat(kvTuneInfo.isForceNoCache())
                .isFalse();
        assertThat(kvTuneInfo.getDescription())
                .isEqualTo("test mount");
        assertThat(kvTuneInfo.getAuditNonHmacRequestKeys())
                .contains("key1", "key2");
        assertThat(kvTuneInfo.getAuditNonHmacResponseKeys())
                .contains("key3", "key4");
        assertThat(kvTuneInfo.getListingVisibility())
                .isEqualTo("hidden");
        assertThat(kvTuneInfo.getPassthroughRequestHeaders())
                .contains("header1", "header2");
        assertThat(kvTuneInfo.getAllowedResponseHeaders())
                .contains("header3", "header4");
        assertThat(kvTuneInfo.getAllowedManagedKeys())
                .contains("key5", "key6");
    }
}

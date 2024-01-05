package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsEnableConfig;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsListingVisibility;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsTuneParams;
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
    public void testRead(VaultClient client, @Random String mount) {
        var mountApi = client.sys().mounts();

        mountApi.enable(mount, "kv", "test kv", new VaultSysMountsEnableConfig()
                .setDefaultLeaseTtl(Duration.ofMinutes(1))
                .setMaxLeaseTtl(Duration.ofMinutes(10))
                .setForceNoCache(true)
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility(VaultSysMountsListingVisibility.HIDDEN)
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setAllowedManagedKeys(List.of("key5", "key6"))
                .setOptions(Map.of("version", "2")))
                .await().indefinitely();

        var kvMountInfo = mountApi.read(mount)
                .await().indefinitely();

        assertThat(kvMountInfo.getAccessor())
                .startsWith("kv_");
        assertThat(kvMountInfo.getConfig())
                .isNotNull();
        assertThat(kvMountInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(kvMountInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(kvMountInfo.getConfig().isForceNoCache())
                .isTrue();
        assertThat(kvMountInfo.getConfig().getAuditNonHmacRequestKeys())
                .contains("key1", "key2");
        assertThat(kvMountInfo.getConfig().getAuditNonHmacResponseKeys())
                .contains("key3", "key4");
        assertThat(kvMountInfo.getConfig().getListingVisibility())
                .isEqualTo(VaultSysMountsListingVisibility.HIDDEN);
        assertThat(kvMountInfo.getConfig().getPassthroughRequestHeaders())
                .contains("header1", "header2");
        assertThat(kvMountInfo.getConfig().getAllowedResponseHeaders())
                .contains("header3", "header4");
        assertThat(kvMountInfo.getConfig().getAllowedManagedKeys())
                .contains("key5", "key6");
        assertThat(kvMountInfo.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(kvMountInfo.getDescription())
                .isEqualTo("test kv");
        assertThat(kvMountInfo.isExternalEntropyAccess())
                .isFalse();
        assertThat(kvMountInfo.isLocal())
                .isFalse();
        assertThat(kvMountInfo.getOptions())
                .isEmpty();
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

        mountApi.enable(path, "kv", null, new VaultSysMountsEnableConfig()
                .setOptions(Map.of("version", "1")))
                .await().indefinitely();

        mountApi.tune(path, new VaultSysMountsTuneParams()
                .setDescription("test mount")
                .setDefaultLeaseTtl(Duration.ofSeconds(90))
                .setMaxLeaseTtl(Duration.ofMinutes(2))
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility(VaultSysMountsListingVisibility.HIDDEN)
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setAllowedManagedKeys(List.of("key5", "key6"))
                .setOptions(Map.of("version", "2")))
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
                .isEqualTo(VaultSysMountsListingVisibility.HIDDEN);
        assertThat(kvTuneInfo.getPassthroughRequestHeaders())
                .contains("header1", "header2");
        assertThat(kvTuneInfo.getAllowedResponseHeaders())
                .contains("header3", "header4");
        assertThat(kvTuneInfo.getAllowedManagedKeys())
                .contains("key5", "key6");
        assertThat(kvTuneInfo.getOptions())
                .containsEntry("version", "2");
    }
}

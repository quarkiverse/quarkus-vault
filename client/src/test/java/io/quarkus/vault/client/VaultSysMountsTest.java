package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(kvMountInfo.accessor)
                .startsWith("kv_");
        assertThat(kvMountInfo.config)
                .isNotNull();
        assertThat(kvMountInfo.config.defaultLeaseTtl)
                .isEqualTo(0);
        assertThat(kvMountInfo.config.maxLeaseTtl)
                .isEqualTo(0);
        assertThat(kvMountInfo.config.forceNoCache)
                .isFalse();
        assertThat(kvMountInfo.deprecationStatus)
                .isEqualTo("supported");
        assertThat(kvMountInfo.description)
                .isEqualTo("key/value secret storage");
        assertThat(kvMountInfo.externalEntropyAccess)
                .isFalse();
        assertThat(kvMountInfo.local)
                .isFalse();
        assertThat(kvMountInfo.options)
                .isNotNull()
                .containsEntry("version", "2");
        assertThat(kvMountInfo.pluginVersion)
                .isEmpty();
        assertThat(kvMountInfo.runningPluginVersion)
                .startsWith("v")
                .endsWith("+builtin");
        assertThat(kvMountInfo.runningSha256)
                .isEmpty();
        assertThat(kvMountInfo.sealWrap)
                .isFalse();
        assertThat(kvMountInfo.type)
                .isEqualTo("kv");
        assertThat(kvMountInfo.uuid)
                .isNotEmpty();
    }

    @Test
    public void testRead(VaultClient client) {
        var mountApi = client.sys().mounts();

        var kvMountInfo = mountApi.read("secret/")
                .await().indefinitely();

        assertThat(kvMountInfo.accessor)
                .startsWith("kv_");
        assertThat(kvMountInfo.config)
                .isNotNull();
        assertThat(kvMountInfo.config.defaultLeaseTtl)
                .isEqualTo(0);
        assertThat(kvMountInfo.config.maxLeaseTtl)
                .isEqualTo(0);
        assertThat(kvMountInfo.config.forceNoCache)
                .isFalse();
        assertThat(kvMountInfo.deprecationStatus)
                .isEqualTo("supported");
        assertThat(kvMountInfo.description)
                .isEqualTo("key/value secret storage");
        assertThat(kvMountInfo.externalEntropyAccess)
                .isFalse();
        assertThat(kvMountInfo.local)
                .isFalse();
        assertThat(kvMountInfo.options)
                .isNotNull()
                .containsEntry("version", "2");
        assertThat(kvMountInfo.pluginVersion)
                .isEmpty();
        assertThat(kvMountInfo.runningPluginVersion)
                .startsWith("v")
                .endsWith("+builtin");
        assertThat(kvMountInfo.runningSha256)
                .isEmpty();
        assertThat(kvMountInfo.sealWrap)
                .isFalse();
        assertThat(kvMountInfo.type)
                .isEqualTo("kv");
        assertThat(kvMountInfo.uuid)
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

        assertThat(kvTuneInfo.description)
                .isEqualTo("key/value secret storage");
        assertThat(kvTuneInfo.defaultLeaseTtl)
                .isEqualTo(2764800L);
        assertThat(kvTuneInfo.maxLeaseTtl)
                .isEqualTo(2764800L);
        assertThat(kvTuneInfo.forceNoCache)
                .isFalse();
        assertThat(kvTuneInfo.allowedManagedKeys)
                .isNull();
        assertThat(kvTuneInfo.auditNonHmacRequestKeys)
                .isNull();
        assertThat(kvTuneInfo.auditNonHmacResponseKeys)
                .isNull();
        assertThat(kvTuneInfo.listingVisibility)
                .isNull();
        assertThat(kvTuneInfo.passthroughRequestHeaders)
                .isNull();
        assertThat(kvTuneInfo.allowedResponseHeaders)
                .isNull();
    }

    @Test
    public void testTune(VaultClient client, @Random String path) {
        var mountApi = client.sys().mounts();

        mountApi.enable(path, "kv", null, null)
                .await().indefinitely();

        mountApi.tune(path, new VaultSysMountsTuneOptions()
                .setDescription("test mount")
                .setDefaultLeaseTtl("90s")
                .setMaxLeaseTtl("120s")
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility("hidden")
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setAllowedManagedKeys(List.of("key5", "key6")))
                .await().indefinitely();

        var kvTuneInfo = mountApi.readTune(path)
                .await().indefinitely();

        assertThat(kvTuneInfo.defaultLeaseTtl)
                .isEqualTo(90L);
        assertThat(kvTuneInfo.maxLeaseTtl)
                .isEqualTo(120L);
        assertThat(kvTuneInfo.forceNoCache)
                .isFalse();
        assertThat(kvTuneInfo.description)
                .isEqualTo("test mount");
        assertThat(kvTuneInfo.auditNonHmacRequestKeys)
                .contains("key1", "key2");
        assertThat(kvTuneInfo.auditNonHmacResponseKeys)
                .contains("key3", "key4");
        assertThat(kvTuneInfo.listingVisibility)
                .isEqualTo("hidden");
        assertThat(kvTuneInfo.passthroughRequestHeaders)
                .contains("header1", "header2");
        assertThat(kvTuneInfo.allowedResponseHeaders)
                .contains("header3", "header4");
        assertThat(kvTuneInfo.allowedManagedKeys)
                .contains("key5", "key6");
    }
}

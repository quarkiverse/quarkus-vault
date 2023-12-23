package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.sys.auth.VaultSysAuthTuneOptions;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysAuthTest {

    @Test
    public void testList(VaultClient client) {
        var authApi = client.sys().auth();

        var auths = authApi.list()
                .await().indefinitely();

        assertThat(auths)
                .isNotNull()
                .containsKey("token/");

        var tokenAuthInfo = auths.get("token/");

        assertThat(tokenAuthInfo.getAccessor())
                .startsWith("auth_token_");
        assertThat(tokenAuthInfo.getConfig())
                .isNotNull();
        assertThat(tokenAuthInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(0);
        assertThat(tokenAuthInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(0);
        assertThat(tokenAuthInfo.getConfig().isForceNoCache())
                .isFalse();
        assertThat(tokenAuthInfo.getDeprecationStatus())
                .isNull();
        assertThat(tokenAuthInfo.getDescription())
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.isExternalEntropyAccess())
                .isFalse();
        assertThat(tokenAuthInfo.isLocal())
                .isFalse();
        assertThat(tokenAuthInfo.getPluginVersion())
                .isEmpty();
        assertThat(tokenAuthInfo.getRunningPluginVersion())
                .startsWith("v")
                .endsWith("+builtin.vault");
        assertThat(tokenAuthInfo.getRunningSha256())
                .isEmpty();
        assertThat(tokenAuthInfo.isSealWrap())
                .isFalse();
        assertThat(tokenAuthInfo.getType())
                .isEqualTo("token");
        assertThat(tokenAuthInfo.getUuid())
                .isNotEmpty();
    }

    @Test
    public void testRead(VaultClient client) {
        var authApi = client.sys().auth();

        var tokenAuthInfo = authApi.read("token/")
                .await().indefinitely();

        assertThat(tokenAuthInfo.getAccessor())
                .startsWith("auth_token_");
        assertThat(tokenAuthInfo.getConfig())
                .isNotNull();
        assertThat(tokenAuthInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(0);
        assertThat(tokenAuthInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(0);
        assertThat(tokenAuthInfo.getConfig().isForceNoCache())
                .isFalse();
        assertThat(tokenAuthInfo.getConfig().getTokenType())
                .isEqualTo("default-service");
        assertThat(tokenAuthInfo.getDeprecationStatus())
                .isNull();
        assertThat(tokenAuthInfo.getDescription())
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.isExternalEntropyAccess())
                .isFalse();
        assertThat(tokenAuthInfo.isLocal())
                .isFalse();
        assertThat(tokenAuthInfo.getPluginVersion())
                .isEmpty();
        assertThat(tokenAuthInfo.getRunningPluginVersion())
                .startsWith("v")
                .endsWith("+builtin.vault");
        assertThat(tokenAuthInfo.getRunningSha256())
                .isEmpty();
        assertThat(tokenAuthInfo.isSealWrap())
                .isFalse();
        assertThat(tokenAuthInfo.getType())
                .isEqualTo("token");
        assertThat(tokenAuthInfo.getUuid())
                .isNotEmpty();
    }

    @Test
    public void testEnable(VaultClient client, @Random String path) {
        var authApi = client.sys().auth();

        var authPath = path + "/";

        authApi.enable(path, "userpass", null, null)
                .await().indefinitely();

        var auths = authApi.list()
                .await().indefinitely();

        assertThat(auths)
                .containsKey(authPath);
    }

    @Test
    public void testDisable(VaultClient client, @Random String path) {
        var authApi = client.sys().auth();

        var authPath = path + "/";

        authApi.enable(path, "userpass", null, null)
                .await().indefinitely();

        var auths = authApi.list()
                .await().indefinitely();

        assertThat(auths)
                .containsKey(authPath);

        authApi.disable(path)
                .await().indefinitely();

        auths = authApi.list()
                .await().indefinitely();

        assertThat(auths)
                .doesNotContainKey(authPath);
    }

    @Test
    public void testReadTune(VaultClient client) {
        var authApi = client.sys().auth();

        var tokenAuthInfo = authApi.readTune("token")
                .await().indefinitely();

        assertThat(tokenAuthInfo.getDescription())
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.getDefaultLeaseTtl())
                .isEqualTo(2764800L);
        assertThat(tokenAuthInfo.getMaxLeaseTtl())
                .isEqualTo(2764800L);
        assertThat(tokenAuthInfo.isForceNoCache())
                .isFalse();
        assertThat(tokenAuthInfo.getAuditNonHmacRequestKeys())
                .isNull();
        assertThat(tokenAuthInfo.getAuditNonHmacResponseKeys())
                .isNull();
        assertThat(tokenAuthInfo.getListingVisibility())
                .isNull();
        assertThat(tokenAuthInfo.getPassthroughRequestHeaders())
                .isNull();
        assertThat(tokenAuthInfo.getAllowedResponseHeaders())
                .isNull();
        assertThat(tokenAuthInfo.getTokenType())
                .isEqualTo("default-service");
    }

    @Test
    public void testTune(VaultClient client, @Random String path) {
        var authApi = client.sys().auth();

        authApi.enable(path, "userpass", "test mount", null)
                .await().indefinitely();

        authApi.tune(path, new VaultSysAuthTuneOptions()
                .setDescription("test mount")
                .setDefaultLeaseTtl("90s")
                .setMaxLeaseTtl("120s")
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility("hidden")
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setTokenType("service"))
                .await().indefinitely();

        var kvTuneInfo = authApi.readTune(path)
                .await().indefinitely();

        assertThat(kvTuneInfo.getDefaultLeaseTtl())
                .isEqualTo(90L);
        assertThat(kvTuneInfo.getMaxLeaseTtl())
                .isEqualTo(120L);
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
        assertThat(kvTuneInfo.getTokenType())
                .isEqualTo("service");
    }
}

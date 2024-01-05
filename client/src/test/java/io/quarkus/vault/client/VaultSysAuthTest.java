package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.common.VaultTokenType;
import io.quarkus.vault.client.api.sys.auth.VaultSysAuthEnableConfig;
import io.quarkus.vault.client.api.sys.auth.VaultSysAuthListingVisibility;
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
                .isEqualTo(Duration.ZERO);
        assertThat(tokenAuthInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(Duration.ZERO);
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
    public void testRead(VaultClient client, @Random String mount) {
        var authApi = client.sys().auth();

        authApi.enable(mount, "userpass", "userpass auth", new VaultSysAuthEnableConfig()
                .setDefaultLeaseTtl(Duration.ofMinutes(1))
                .setMaxLeaseTtl(Duration.ofMinutes(10))
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setAllowedResponseHeaders(List.of("header1", "header2"))
                .setListingVisibility(VaultSysAuthListingVisibility.HIDDEN)
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4")))
                .await().indefinitely();

        var tokenAuthInfo = authApi.read(mount)
                .await().indefinitely();

        assertThat(tokenAuthInfo.getAccessor())
                .startsWith("auth_userpass_");
        assertThat(tokenAuthInfo.getConfig())
                .isNotNull();
        assertThat(tokenAuthInfo.getConfig().getDefaultLeaseTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(tokenAuthInfo.getConfig().getMaxLeaseTtl())
                .isEqualTo(Duration.ofMinutes(10));
        assertThat(tokenAuthInfo.getConfig().isForceNoCache())
                .isFalse();
        assertThat(tokenAuthInfo.getConfig().getAuditNonHmacRequestKeys())
                .contains("key1", "key2");
        assertThat(tokenAuthInfo.getConfig().getAuditNonHmacResponseKeys())
                .contains("key3", "key4");
        assertThat(tokenAuthInfo.getConfig().getListingVisibility())
                .isEqualTo(VaultSysAuthListingVisibility.HIDDEN);
        assertThat(tokenAuthInfo.getConfig().getPassthroughRequestHeaders())
                .contains("header1", "header2");
        assertThat(tokenAuthInfo.getConfig().getAllowedResponseHeaders())
                .contains("header3", "header4");
        assertThat(tokenAuthInfo.getConfig().getTokenType())
                .isEqualTo(VaultTokenType.DEFAULT_SERVICE);
        assertThat(tokenAuthInfo.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(tokenAuthInfo.getDescription())
                .isEqualTo("userpass auth");
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
                .isEqualTo("userpass");
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
                .isEqualTo(Duration.ofDays(32));
        assertThat(tokenAuthInfo.getMaxLeaseTtl())
                .isEqualTo(Duration.ofDays(32));
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
                .isEqualTo(VaultTokenType.DEFAULT_SERVICE);
    }

    @Test
    public void testTune(VaultClient client, @Random String path) {
        var authApi = client.sys().auth();

        authApi.enable(path, "userpass", "test mount", null)
                .await().indefinitely();

        authApi.tune(path, new VaultSysAuthTuneOptions()
                .setDescription("test mount")
                .setDefaultLeaseTtl(Duration.ofSeconds(90))
                .setMaxLeaseTtl(Duration.ofMinutes(2))
                .setAuditNonHmacRequestKeys(List.of("key1", "key2"))
                .setAuditNonHmacResponseKeys(List.of("key3", "key4"))
                .setListingVisibility(VaultSysAuthListingVisibility.HIDDEN)
                .setPassthroughRequestHeaders(List.of("header1", "header2"))
                .setAllowedResponseHeaders(List.of("header3", "header4"))
                .setTokenType(VaultTokenType.SERVICE))
                .await().indefinitely();

        var kvTuneInfo = authApi.readTune(path)
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
                .isEqualTo(VaultSysAuthListingVisibility.HIDDEN);
        assertThat(kvTuneInfo.getPassthroughRequestHeaders())
                .contains("header1", "header2");
        assertThat(kvTuneInfo.getAllowedResponseHeaders())
                .contains("header3", "header4");
        assertThat(kvTuneInfo.getTokenType())
                .isEqualTo(VaultTokenType.SERVICE);
    }
}

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

        assertThat(tokenAuthInfo.accessor)
                .startsWith("auth_token_");
        assertThat(tokenAuthInfo.config)
                .isNotNull();
        assertThat(tokenAuthInfo.config.defaultLeaseTtl)
                .isEqualTo(0);
        assertThat(tokenAuthInfo.config.maxLeaseTtl)
                .isEqualTo(0);
        assertThat(tokenAuthInfo.config.forceNoCache)
                .isFalse();
        assertThat(tokenAuthInfo.deprecationStatus)
                .isNull();
        assertThat(tokenAuthInfo.description)
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.externalEntropyAccess)
                .isFalse();
        assertThat(tokenAuthInfo.local)
                .isFalse();
        assertThat(tokenAuthInfo.pluginVersion)
                .isEmpty();
        assertThat(tokenAuthInfo.runningPluginVersion)
                .startsWith("v")
                .endsWith("+builtin.vault");
        assertThat(tokenAuthInfo.runningSha256)
                .isEmpty();
        assertThat(tokenAuthInfo.sealWrap)
                .isFalse();
        assertThat(tokenAuthInfo.type)
                .isEqualTo("token");
        assertThat(tokenAuthInfo.uuid)
                .isNotEmpty();
    }

    @Test
    public void testRead(VaultClient client) {
        var authApi = client.sys().auth();

        var tokenAuthInfo = authApi.read("token/")
                .await().indefinitely();

        assertThat(tokenAuthInfo.accessor)
                .startsWith("auth_token_");
        assertThat(tokenAuthInfo.config)
                .isNotNull();
        assertThat(tokenAuthInfo.config.defaultLeaseTtl)
                .isEqualTo(0);
        assertThat(tokenAuthInfo.config.maxLeaseTtl)
                .isEqualTo(0);
        assertThat(tokenAuthInfo.config.forceNoCache)
                .isFalse();
        assertThat(tokenAuthInfo.config.tokenType)
                .isEqualTo("default-service");
        assertThat(tokenAuthInfo.deprecationStatus)
                .isNull();
        assertThat(tokenAuthInfo.description)
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.externalEntropyAccess)
                .isFalse();
        assertThat(tokenAuthInfo.local)
                .isFalse();
        assertThat(tokenAuthInfo.pluginVersion)
                .isEmpty();
        assertThat(tokenAuthInfo.runningPluginVersion)
                .startsWith("v")
                .endsWith("+builtin.vault");
        assertThat(tokenAuthInfo.runningSha256)
                .isEmpty();
        assertThat(tokenAuthInfo.sealWrap)
                .isFalse();
        assertThat(tokenAuthInfo.type)
                .isEqualTo("token");
        assertThat(tokenAuthInfo.uuid)
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

        assertThat(tokenAuthInfo.description)
                .isEqualTo("token based credentials");
        assertThat(tokenAuthInfo.defaultLeaseTtl)
                .isEqualTo(2764800L);
        assertThat(tokenAuthInfo.maxLeaseTtl)
                .isEqualTo(2764800L);
        assertThat(tokenAuthInfo.forceNoCache)
                .isFalse();
        assertThat(tokenAuthInfo.auditNonHmacRequestKeys)
                .isNull();
        assertThat(tokenAuthInfo.auditNonHmacResponseKeys)
                .isNull();
        assertThat(tokenAuthInfo.listingVisibility)
                .isNull();
        assertThat(tokenAuthInfo.passthroughRequestHeaders)
                .isNull();
        assertThat(tokenAuthInfo.allowedResponseHeaders)
                .isNull();
        assertThat(tokenAuthInfo.tokenType)
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
        assertThat(kvTuneInfo.tokenType)
                .isEqualTo("service");
    }
}

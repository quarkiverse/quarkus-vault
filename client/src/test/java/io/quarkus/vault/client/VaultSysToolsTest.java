package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.common.VaultFormat;
import io.quarkus.vault.client.api.common.VaultHashAlgorithm;
import io.quarkus.vault.client.api.common.VaultRandomSource;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysToolsTest {

    @Test
    public void testRandomBase64(VaultClient client) {
        var toolsApi = client.sys().tools();

        var random = toolsApi.random(32, VaultRandomSource.ALL, VaultFormat.BASE64)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testRandomHex(VaultClient client) {
        var toolsApi = client.sys().tools();

        var random = toolsApi.random(32, VaultRandomSource.PLATFORM, VaultFormat.HEX)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testRandomWithDefaults(VaultClient client) {
        var toolsApi = client.sys().tools();

        var random = toolsApi.random(32, null, null)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testHash(VaultClient client) throws NoSuchAlgorithmException {
        var toolsApi = client.sys().tools();

        var data = "test".getBytes();

        var hash = toolsApi.hash(VaultHashAlgorithm.SHA2_512, data, VaultFormat.BASE64)
                .await().indefinitely();

        var localHash = Base64.getEncoder().encodeToString(MessageDigest.getInstance("SHA-512").digest(data));

        assertThat(hash)
                .isEqualTo(localHash);
    }

    @Test
    public void testHashDefaultParams(VaultClient client) throws NoSuchAlgorithmException {
        var toolsApi = client.sys().tools();

        var data = "test".getBytes();

        var hash = toolsApi.hash(null, data, null)
                .await().indefinitely();

        var localHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));

        assertThat(hash)
                .isEqualTo(localHash);
    }

}

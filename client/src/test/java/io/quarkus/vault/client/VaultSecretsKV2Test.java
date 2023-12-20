package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2SecretMetadataParams;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsEnableOptions;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.EngineMount;

@VaultClientTest({
        @EngineMount(engine = "kv", path = "kv-v2", options = "-version=2"),
})
public class VaultSecretsKV2Test {

    @Test
    void testConfig(VaultClient client) {

        var config = client.secrets().kv2("kv-v2").readConfig()
                .await().indefinitely();

        assertThat(config.data).isNotNull();
        assertThat(config.data.maxVersions).isEqualTo(0);
    }

    @Test
    void testUpdateConfig(VaultClient client, @Random String path) {
        // Mount specific engine for testing CAS configuration
        client.sys().mounts().enable(path, "kv", "KV with CAS enabled",
                new VaultSysMountsEnableOptions()
                        .setOptions(Map.of("version", "2")))
                .await().indefinitely();

        var kvApi = client.secrets().kv2(path);

        kvApi.updateConfig(3, true, "3m0s")
                .await().indefinitely();

        var config = kvApi.readConfig()
                .await().indefinitely();

        assertThat(config.data).isNotNull();
        assertThat(config.data.maxVersions).isEqualTo(3);
        assertThat(config.data.deleteVersionAfter).isEqualTo("3m0s");
        assertThat(config.data.casRequired).isTrue();
    }

    @Test
    void testUpdate(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        var readResult = kvApi.readSecret(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.data).isNotNull()
                .hasSize(2)
                .containsEntry("greeting", "hello")
                .containsEntry("subject", "world");
        assertThat(data.metadata).isNotNull();
        assertThat(data.metadata.version).isEqualTo(1);
        assertThat(data.metadata.createdTime)
                .isNotNull()
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
    }

    @Test
    void testList(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path + "/test1", null, Map.of("key1", "val1", "key2", "val2"))
                .await().indefinitely();
        kvApi.updateSecret(path + "/test2", null, Map.of("key1", "val1", "key2", "val2"))
                .await().indefinitely();

        var listResult = kvApi.listSecrets(path)
                .await().indefinitely();

        var data = listResult.data;

        assertThat(data).isNotNull();
        assertThat(data.keys).isNotNull()
                .contains("test1", "test2");
    }

    @Test
    void testDelete(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("test", "some-value"))
                .await().indefinitely();

        // Validate update
        var readResult = kvApi.readSecret(path)
                .await().indefinitely();

        assertThat(readResult.data).isNotNull();
        assertThat(readResult.data.data).isNotNull().containsEntry("test", "some-value");

        // Delete and validate

        kvApi.deleteSecret(path)
                .await().indefinitely();

        assertThatThrownBy(() -> kvApi.readSecret(path).await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

    @Test
    void testReadWithVersion(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        {
            var readResult = kvApi.readSecret(path, 1)
                    .await().indefinitely();

            var data = readResult.data;

            assertThat(data).isNotNull();
            assertThat(data.data).isNotNull()
                    .hasSize(1)
                    .containsEntry("initial", "value");
            assertThat(data.metadata).isNotNull();
            assertThat(data.metadata.version).isEqualTo(1);
            assertThat(data.metadata.createdTime)
                    .isNotNull()
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        }

        {
            var readResult = kvApi.readSecret(path, 2)
                    .await().indefinitely();

            var data = readResult.data;

            assertThat(data).isNotNull();
            assertThat(data.data).isNotNull()
                    .hasSize(2)
                    .containsEntry("greeting", "hello")
                    .containsEntry("subject", "world");
            assertThat(data.metadata).isNotNull();
            assertThat(data.metadata.version).isEqualTo(2);
            assertThat(data.metadata.createdTime)
                    .isNotNull()
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        }
    }

    @Test
    void testUpdateMetadata(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .await().indefinitely();

        var readResult = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.casRequired).isFalse();
        assertThat(data.createdTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.currentVersion).isEqualTo(1);
        assertThat(data.deleteVersionAfter).isEqualTo("3m0s");
        assertThat(data.maxVersions).isEqualTo(3);
        assertThat(data.oldestVersion).isEqualTo(0);
        assertThat(data.updatedTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.customMetadata)
                .isNotNull()
                .hasSize(1)
                .containsEntry("owner", "quarkus-vault-client-test");
        assertThat(data.versions)
                .hasSize(1)
                .containsKey("1");
        assertThat(data.versions.get("1").createdTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.versions.get("1").destroyed).isFalse();
        assertThat(data.versions.get("1").deletionTime).isNull();
    }

    @Test
    void testPatchMetadata(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.patchSecretMetadata(path, new VaultSecretsKV2SecretMetadataParams()
                .setCustomMetadata(Map.of("owner", "quarkus-vault-client-test")))
                .await().indefinitely();

        var readResult = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.casRequired).isFalse();
        assertThat(data.createdTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.currentVersion).isEqualTo(1);
        assertThat(data.deleteVersionAfter).isEqualTo("0s");
        assertThat(data.maxVersions).isEqualTo(0);
        assertThat(data.oldestVersion).isEqualTo(0);
        assertThat(data.updatedTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.customMetadata)
                .isNotNull()
                .hasSize(1)
                .containsEntry("owner", "quarkus-vault-client-test");
        assertThat(data.versions)
                .hasSize(1)
                .containsKey("1");
        assertThat(data.versions.get("1").createdTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(data.versions.get("1").destroyed).isFalse();
        assertThat(data.versions.get("1").deletionTime).isNull();
    }

    @Test
    void testReadMetadataAfterDelete(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .await().indefinitely();
        kvApi.deleteSecret(path)
                .await().indefinitely();

        var readResult = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.versions).isNotNull().containsKey("1");
        assertThat(data.versions.get("1").destroyed).isFalse();
        assertThat(data.versions.get("1").deletionTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
    }

    @Test
    void testDeleteVersion(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .await().indefinitely();
        kvApi.deleteSecretVersions(path, List.of(1))
                .await().indefinitely();

        var readResult = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.versions).isNotNull().containsKey("1");
        assertThat(data.versions.get("1").destroyed).isFalse();
        assertThat(data.versions.get("1").deletionTime).isBetween(now().minusSeconds(2), now().plusSeconds(2));
    }

    @Test
    void testDestroyVersion(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .await().indefinitely();
        kvApi.destroySecretVersions(path, List.of(1))
                .await().indefinitely();

        var readResult = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        var data = readResult.data;

        assertThat(data).isNotNull();
        assertThat(data.versions).isNotNull().containsKey("1");
        assertThat(data.versions.get("1").destroyed).isTrue();
        assertThat(data.versions.get("1").deletionTime).isNull();
    }

    @Test
    void testDeleteMetadataAndHistory(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();

        var readResult = kvApi.readSecret(path)
                .await().indefinitely();
        var data = readResult.data;
        assertThat(data).isNotNull();

        kvApi.deleteSecretMetadata(path)
                .await().indefinitely();

        assertThatThrownBy(() -> kvApi.readSecret(path).await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

}

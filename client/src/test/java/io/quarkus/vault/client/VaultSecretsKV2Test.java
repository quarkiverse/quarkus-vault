package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2SecretMetadataParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(secrets = {
        @Mount(type = "kv", path = "kv-v2", options = "-version=2"),
})
public class VaultSecretsKV2Test {

    @Test
    void testReadConfig(VaultClient client) {

        var config = client.secrets().kv2("kv-v2").readConfig()
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.getMaxVersions())
                .isEqualTo(0);
    }

    @Test
    void testUpdateConfig(VaultClient client, @Random String path) {
        // Mount specific engine for testing CAS configuration
        client.sys().mounts().enable(path, "kv", "KV with CAS enabled", null, Map.of("version", "2"))
                .await().indefinitely();

        var kvApi = client.secrets().kv2(path);

        kvApi.updateConfig(3, true, "3m0s")
                .await().indefinitely();

        var config = kvApi.readConfig()
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.getMaxVersions())
                .isEqualTo(3);
        assertThat(config.getDeleteVersionAfter())
                .isEqualTo("3m0s");
        assertThat(config.isCasRequired())
                .isTrue();
    }

    @Test
    void testUpdate(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        var secret = kvApi.readSecret(path)
                .await().indefinitely();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .isNotNull()
                .hasSize(2)
                .containsEntry("greeting", "hello")
                .containsEntry("subject", "world");
        assertThat(secret.getMetadata())
                .isNotNull();
        assertThat(secret.getMetadata().getVersion())
                .isEqualTo(1);
        assertThat(secret.getMetadata().getCreatedTime())
                .isNotNull()
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
    }

    @Test
    void testPatch(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        kvApi.patchSecret(path, null, Map.of("subject", "new world!"))
                .await().indefinitely();

        var secret = kvApi.readSecret(path)
                .await().indefinitely();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .isNotNull()
                .hasSize(2)
                .containsEntry("greeting", "hello")
                .containsEntry("subject", "new world!");
    }

    @Test
    void testReadSubkeys(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        var read = kvApi.readSubkeys(path)
                .await().indefinitely();

        assertThat(read.getSubkeys())
                .isNotNull()
                .containsEntry("greeting", null)
                .containsEntry("subject", null);
    }

    @Test
    void testList(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path + "/test1", null, Map.of("key1", "val1", "key2", "val2"))
                .await().indefinitely();
        kvApi.updateSecret(path + "/test2", null, Map.of("key1", "val1", "key2", "val2"))
                .await().indefinitely();

        var keys = kvApi.listSecrets(path)
                .await().indefinitely();

        assertThat(keys)
                .isNotNull()
                .contains("test1", "test2");
    }

    @Test
    void testListEmpty(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        var keys = kvApi.listSecrets(path)
                .await().indefinitely();

        assertThat(keys)
                .isEmpty();
    }

    @Test
    void testDelete(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("test", "some-value"))
                .await().indefinitely();

        // Validate update
        var secret = kvApi.readSecret(path)
                .await().indefinitely();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .isNotNull()
                .containsEntry("test", "some-value");

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
            var secret = kvApi.readSecret(path, 1)
                    .await().indefinitely();

            assertThat(secret)
                    .isNotNull();
            assertThat(secret.getData())
                    .isNotNull()
                    .hasSize(1)
                    .containsEntry("initial", "value");
            assertThat(secret.getMetadata())
                    .isNotNull();
            assertThat(secret.getMetadata().getVersion())
                    .isEqualTo(1);
            assertThat(secret.getMetadata().getCreatedTime())
                    .isNotNull()
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        }

        {
            var secret = kvApi.readSecret(path, 2)
                    .await().indefinitely();

            assertThat(secret)
                    .isNotNull();
            assertThat(secret.getData())
                    .isNotNull()
                    .hasSize(2)
                    .containsEntry("greeting", "hello")
                    .containsEntry("subject", "world");
            assertThat(secret.getMetadata())
                    .isNotNull();
            assertThat(secret.getMetadata().getVersion())
                    .isEqualTo(2);
            assertThat(secret.getMetadata().getCreatedTime())
                    .isNotNull()
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        }
    }

    @Test
    void testReadSubkeysWithVersion(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .await().indefinitely();

        var read = kvApi.readSubkeys(path, 2, null)
                .await().indefinitely();

        assertThat(read.getSubkeys())
                .isNotNull()
                .containsEntry("greeting", null)
                .containsEntry("subject", null);
    }

    @Test
    void testUpdateMetadata(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .await().indefinitely();

        var metadata = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        assertThat(metadata)
                .isNotNull();
        assertThat(metadata.isCasRequired())
                .isFalse();
        assertThat(metadata.getCreatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getCurrentVersion())
                .isEqualTo(1);
        assertThat(metadata.getDeleteVersionAfter())
                .isEqualTo("3m0s");
        assertThat(metadata.getMaxVersions())
                .isEqualTo(3);
        assertThat(metadata.getOldestVersion())
                .isEqualTo(0);
        assertThat(metadata.getUpdatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getCustomMetadata())
                .isNotNull()
                .hasSize(1)
                .containsEntry("owner", "quarkus-vault-client-test");
        assertThat(metadata.getVersions())
                .hasSize(1)
                .containsKey("1");
        assertThat(metadata.getVersions().get("1").getCreatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getVersions().get("1").isDestroyed())
                .isFalse();
        assertThat(metadata.getVersions().get("1").getDeletionTime())
                .isNull();
    }

    @Test
    void testPatchMetadata(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.patchSecretMetadata(path, new VaultSecretsKV2SecretMetadataParams()
                .setCustomMetadata(Map.of("owner", "quarkus-vault-client-test")))
                .await().indefinitely();

        var metadata = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        assertThat(metadata)
                .isNotNull();
        assertThat(metadata.isCasRequired())
                .isFalse();
        assertThat(metadata.getCreatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getCurrentVersion())
                .isEqualTo(1);
        assertThat(metadata.getDeleteVersionAfter())
                .isEqualTo("0s");
        assertThat(metadata.getMaxVersions())
                .isEqualTo(0);
        assertThat(metadata.getOldestVersion())
                .isEqualTo(0);
        assertThat(metadata.getUpdatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getCustomMetadata())
                .isNotNull()
                .hasSize(1)
                .containsEntry("owner", "quarkus-vault-client-test");
        assertThat(metadata.getVersions())
                .hasSize(1)
                .containsKey("1");
        assertThat(metadata.getVersions().get("1").getCreatedTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(metadata.getVersions().get("1").isDestroyed())
                .isFalse();
        assertThat(metadata.getVersions().get("1").getDeletionTime())
                .isNull();
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

        var metadata = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        assertThat(metadata)
                .isNotNull();
        assertThat(metadata.getVersions())
                .isNotNull().containsKey("1");
        assertThat(metadata.getVersions().get("1").isDestroyed())
                .isFalse();
        assertThat(metadata.getVersions().get("1").getDeletionTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
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

        var metadata = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        assertThat(metadata)
                .isNotNull();
        assertThat(metadata.getVersions())
                .isNotNull()
                .containsKey("1");
        assertThat(metadata.getVersions().get("1").isDestroyed())
                .isFalse();
        assertThat(metadata.getVersions().get("1").getDeletionTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
    }

    @Test
    void testUndeleteVersion(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();
        kvApi.deleteSecretVersions(path, List.of(1))
                .await().indefinitely();

        {
            var metadata = kvApi.readSecretMetadata(path)
                    .await().indefinitely();

            assertThat(metadata)
                    .isNotNull();
            assertThat(metadata.getVersions())
                    .isNotNull()
                    .containsKey("1");
            assertThat(metadata.getVersions().get("1").isDestroyed())
                    .isFalse();
            assertThat(metadata.getVersions().get("1").getDeletionTime())
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));

            assertThatThrownBy(() -> kvApi.readSecret(path, 1).await().indefinitely())
                    .isInstanceOf(VaultClientException.class)
                    .asString().contains("status=404");
        }

        kvApi.undeleteSecretVersions(path, List.of(1))
                .await().indefinitely();

        {
            var metadata = kvApi.readSecretMetadata(path)
                    .await().indefinitely();

            assertThat(metadata)
                    .isNotNull();
            assertThat(metadata.getVersions())
                    .isNotNull()
                    .containsKey("1");
            assertThat(metadata.getVersions().get("1").isDestroyed())
                    .isFalse();
            assertThat(metadata.getVersions().get("1").getDeletionTime())
                    .isNull();

            assertThatNoException().isThrownBy(() -> kvApi.readSecret(path, 1).await().indefinitely());
        }
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

        var data = kvApi.readSecretMetadata(path)
                .await().indefinitely();

        assertThat(data)
                .isNotNull();
        assertThat(data.getVersions())
                .isNotNull()
                .containsKey("1");
        assertThat(data.getVersions().get("1").isDestroyed())
                .isTrue();
        assertThat(data.getVersions().get("1").getDeletionTime())
                .isNull();
    }

    @Test
    void testDeleteMetadataAndHistory(VaultClient client, @Random String path) {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .await().indefinitely();

        var secret = kvApi.readSecret(path)
                .await().indefinitely();

        assertThat(secret.getData())
                .isNotNull();

        kvApi.deleteSecretMetadata(path)
                .await().indefinitely();

        assertThatThrownBy(() -> kvApi.readSecret(path).await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

}

package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    public void testReadConfig(VaultClient client) throws Exception {

        var config = client.secrets().kv2("kv-v2").readConfig()
                .toCompletableFuture().get();

        assertThat(config)
                .isNotNull();
        assertThat(config.getMaxVersions())
                .isEqualTo(0);
    }

    @Test
    public void testUpdateConfig(VaultClient client, @Random String path) throws Exception {
        // Mount specific engine for testing CAS configuration
        client.sys().mounts().enable(path, "kv", "KV with CAS enabled", null, Map.of("version", "2"))
                .toCompletableFuture().get();

        var kvApi = client.secrets().kv2(path);

        kvApi.updateConfig(3, true, "3m0s")
                .toCompletableFuture().get();

        var config = kvApi.readConfig()
                .toCompletableFuture().get();

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
    public void testUpdate(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        var secret = kvApi.readSecret(path)
                .toCompletableFuture().get();

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
    public void testPatch(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        kvApi.patchSecret(path, null, Map.of("subject", "new world!"))
                .toCompletableFuture().get();

        var secret = kvApi.readSecret(path)
                .toCompletableFuture().get();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .isNotNull()
                .hasSize(2)
                .containsEntry("greeting", "hello")
                .containsEntry("subject", "new world!");
    }

    @Test
    public void testReadSubkeys(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        var read = kvApi.readSubkeys(path)
                .toCompletableFuture().get();

        assertThat(read.getSubkeys())
                .isNotNull()
                .containsEntry("greeting", null)
                .containsEntry("subject", null);
    }

    @Test
    public void testList(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path + "/test1", null, Map.of("key1", "val1", "key2", "val2"))
                .toCompletableFuture().get();
        kvApi.updateSecret(path + "/test2", null, Map.of("key1", "val1", "key2", "val2"))
                .toCompletableFuture().get();

        var keys = kvApi.listSecrets(path)
                .toCompletableFuture().get();

        assertThat(keys)
                .isNotNull()
                .contains("test1", "test2");
    }

    @Test
    public void testListEmpty(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        var keys = kvApi.listSecrets(path)
                .toCompletableFuture().get();

        assertThat(keys)
                .isEmpty();
    }

    @Test
    public void testDelete(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("test", "some-value"))
                .toCompletableFuture().get();

        // Validate update
        var secret = kvApi.readSecret(path)
                .toCompletableFuture().get();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .isNotNull()
                .containsEntry("test", "some-value");

        // Delete and validate

        kvApi.deleteSecret(path)
                .toCompletableFuture().get();

        assertThatThrownBy(() -> kvApi.readSecret(path).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

    @Test
    public void testReadWithVersion(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        {
            var secret = kvApi.readSecret(path, 1)
                    .toCompletableFuture().get();

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
                    .toCompletableFuture().get();

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
    public void testReadSubkeysWithVersion(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecret(path, null, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        var read = kvApi.readSubkeys(path, 2, null)
                .toCompletableFuture().get();

        assertThat(read.getSubkeys())
                .isNotNull()
                .containsEntry("greeting", null)
                .containsEntry("subject", null);
    }

    @Test
    public void testUpdateMetadata(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .toCompletableFuture().get();

        var metadata = kvApi.readSecretMetadata(path)
                .toCompletableFuture().get();

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
    public void testPatchMetadata(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.patchSecretMetadata(path, new VaultSecretsKV2SecretMetadataParams()
                .setCustomMetadata(Map.of("owner", "quarkus-vault-client-test")))
                .toCompletableFuture().get();

        var metadata = kvApi.readSecretMetadata(path)
                .toCompletableFuture().get();

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
    public void testReadMetadataAfterDelete(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .toCompletableFuture().get();
        kvApi.deleteSecret(path)
                .toCompletableFuture().get();

        var metadata = kvApi.readSecretMetadata(path)
                .toCompletableFuture().get();

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
    public void testDeleteVersion(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .toCompletableFuture().get();
        kvApi.deleteSecretVersions(path, List.of(1))
                .toCompletableFuture().get();

        var metadata = kvApi.readSecretMetadata(path)
                .toCompletableFuture().get();

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
    public void testUndeleteVersion(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.deleteSecretVersions(path, List.of(1))
                .toCompletableFuture().get();

        {
            var metadata = kvApi.readSecretMetadata(path)
                    .toCompletableFuture().get();

            assertThat(metadata)
                    .isNotNull();
            assertThat(metadata.getVersions())
                    .isNotNull()
                    .containsKey("1");
            assertThat(metadata.getVersions().get("1").isDestroyed())
                    .isFalse();
            assertThat(metadata.getVersions().get("1").getDeletionTime())
                    .isBetween(now().minusSeconds(2), now().plusSeconds(2));

            assertThatThrownBy(() -> kvApi.readSecret(path, 1).toCompletableFuture().get())
                    .isInstanceOf(ExecutionException.class).cause()
                    .isInstanceOf(VaultClientException.class)
                    .hasFieldOrPropertyWithValue("status", 404);
        }

        kvApi.undeleteSecretVersions(path, List.of(1))
                .toCompletableFuture().get();

        {
            var metadata = kvApi.readSecretMetadata(path)
                    .toCompletableFuture().get();

            assertThat(metadata)
                    .isNotNull();
            assertThat(metadata.getVersions())
                    .isNotNull()
                    .containsKey("1");
            assertThat(metadata.getVersions().get("1").isDestroyed())
                    .isFalse();
            assertThat(metadata.getVersions().get("1").getDeletionTime())
                    .isNull();

            assertThatNoException().isThrownBy(() -> kvApi.readSecret(path, 1).toCompletableFuture().get());
        }
    }

    @Test
    public void testDestroyVersion(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();
        kvApi.updateSecretMetadata(path, 3, false, "3m", Map.of("owner", "quarkus-vault-client-test"))
                .toCompletableFuture().get();
        kvApi.destroySecretVersions(path, List.of(1))
                .toCompletableFuture().get();

        var data = kvApi.readSecretMetadata(path)
                .toCompletableFuture().get();

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
    public void testDeleteMetadataAndHistory(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv2("kv-v2");

        kvApi.updateSecret(path, null, Map.of("initial", "value"))
                .toCompletableFuture().get();

        var secret = kvApi.readSecret(path)
                .toCompletableFuture().get();

        assertThat(secret.getData())
                .isNotNull();

        kvApi.deleteSecretMetadata(path)
                .toCompletableFuture().get();

        assertThatThrownBy(() -> kvApi.readSecret(path).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

}

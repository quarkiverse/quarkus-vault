package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(secrets = {
        @Mount(type = "kv", path = "kv-v1"),
})
public class VaultSecretsKV1Test {

    @Test
    public void testUpdateRead(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv1("kv-v1");

        kvApi.update(path, Map.of("greeting", "hello", "subject", "world"))
                .toCompletableFuture().get();

        var data = kvApi.read(path)
                .toCompletableFuture().get();

        assertThat(data)
                .isNotNull()
                .hasSize(2)
                .containsEntry("greeting", "hello")
                .containsEntry("subject", "world");
    }

    @Test
    public void testList(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv1("kv-v1");

        kvApi.update(path + "/test1", Map.of("key1", "val1", "key2", "val2"))
                .toCompletableFuture().get();
        kvApi.update(path + "/test2", Map.of("key1", "val1", "key2", "val2"))
                .toCompletableFuture().get();

        var data = kvApi.list(path + "/")
                .toCompletableFuture().get();

        assertThat(data)
                .contains("test1", "test2");
    }

    @Test
    public void testListRoot(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv1("kv-v1");

        kvApi.update(path, Map.of("key1", "val1", "key2", "val2"))
                .toCompletableFuture().get();

        var keys = kvApi.list()
                .toCompletableFuture().get();

        assertThat(keys)
                .isNotNull()
                .contains(path);
    }

    @Test
    public void testDelete(VaultClient client, @Random String path) throws Exception {

        var kvApi = client.secrets().kv1("kv-v1");

        kvApi.update(path, Map.of("test", "some-value"))
                .toCompletableFuture().get();

        // Validate update
        var data = kvApi.read(path)
                .toCompletableFuture().get();

        assertThat(data)
                .isNotNull()
                .containsEntry("test", "some-value");

        // Delete and validate

        kvApi.delete(path)
                .toCompletableFuture().get();

        assertThatThrownBy(() -> kvApi.read(path).toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

}

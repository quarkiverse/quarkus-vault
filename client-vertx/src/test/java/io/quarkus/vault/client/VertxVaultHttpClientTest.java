package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.http.vertx.VertxVaultHttpClient;
import io.vertx.core.Vertx;
import io.vertx.ext.web.client.WebClient;

@Testcontainers
public class VertxVaultHttpClientTest {

    @Container
    public static final VaultContainer<?> vault = new VaultContainer<>("hashicorp/vault:1.15.4")
            .withInitCommand("secrets enable -path=kv1 -version=1 kv")
            .withVaultToken("root");

    private static VertxVaultHttpClient httpClient;
    private static VaultClient client;

    @BeforeAll
    public static void setup() {
        var vertx = Vertx.vertx();
        httpClient = new VertxVaultHttpClient(WebClient.create(vertx));
        client = VaultClient.builder()
                .executor(httpClient)
                .baseUrl(vault.getHttpHostAddress())
                .clientToken("root")
                .build();
    }

    @AfterAll
    public static void teardown() {
        httpClient.close();
    }

    @Test
    public void testGet() {
        var kv1 = client.secrets().kv1("kv1");

        kv1.update("foo", Map.of("bar", "baz"))
                .await().indefinitely();

        var result = kv1.read("foo")
                .await().indefinitely();

        assertThat(result)
                .containsEntry("bar", "baz");
    }

    @Test
    public void testList() {
        var kv1 = client.secrets().kv1("kv1");

        kv1.update("foo", Map.of("bar", "baz"))
                .await().indefinitely();

        var result = kv1.list()
                .await().indefinitely();

        assertThat(result)
                .contains("foo");
    }

    @Test
    public void testDelete() {
        var kv1 = client.secrets().kv1("kv1");

        kv1.update("foo", Map.of("bar", "baz"))
                .await().indefinitely();

        kv1.delete("foo")
                .await().indefinitely();

        assertThatThrownBy(() -> kv1.list().await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

    @Test
    public void testHead() {
        var health = client.sys().health();

        var result = health.statusCode()
                .await().indefinitely();

        assertThat(result)
                .isEqualTo(200);
    }

}

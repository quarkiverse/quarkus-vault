package io.quarkus.vault.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.http.TestHTTPResource;
import tools.jackson.databind.JsonNode;

/**
 * After a refresh that fails because Vault is unreachable, the config source keeps serving the last known values, so
 * the Config sources page must keep showing them, with the refresh error.
 */
@Tag("devmode")
public class VaultDevUIFailedRefreshTest extends DevUIJsonRPCTest {

    private static final long CACHE_PERIOD_MILLIS = 1000;

    private static final HttpServer FAKE_VAULT = startFakeVault();

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar
                    .addClass(GreetingResource.class)
                    .addAsResource(new StringAsset("""
                            quarkus.vault.url=http://localhost:%d
                            quarkus.vault.authentication.client-token=123
                            quarkus.vault.kv-secret-engine-version=1
                            quarkus.vault.secret-config-kv-path=config
                            quarkus.vault.secret-config-cache-period=%dms
                            quarkus.vault.devservices.enabled=false
                            """.formatted(FAKE_VAULT.getAddress().getPort(), CACHE_PERIOD_MILLIS)),
                            "application.properties"));

    @TestHTTPResource("/greeting")
    URI greetingUrl;

    public VaultDevUIFailedRefreshTest() {
        super("quarkus-vault");
    }

    @AfterAll
    static void stopFakeVault() {
        FAKE_VAULT.stop(0);
    }

    @Test
    public void pageKeepsServedPropertiesAfterAFailedRefresh() throws Exception {
        assertEquals("hello", get(greetingUrl));
        JsonNode before = executeJsonRPCMethod("getConfigSources");
        assertEquals("greeting", before.get("paths").get(0).get("propertyNames").get(0).asString(), before.toString());

        FAKE_VAULT.stop(0);
        Thread.sleep(CACHE_PERIOD_MILLIS + 500);

        assertEquals("hello", get(greetingUrl));
        JsonNode after = executeJsonRPCMethod("getConfigSources");
        assertEquals(before.get("fetchedAt"), after.get("fetchedAt"), after.toString());
        assertEquals(before.get("paths"), after.get("paths"), after.toString());
        assertEquals("unreachable", after.get("refreshError").asString(), after.toString());
        assertEquals("error", executeJsonRPCMethod("getConfigSourcesLabel").asString());
    }

    private static String get(URI uri) throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(HttpRequest.newBuilder(uri).GET().build(), BodyHandlers.ofString()).body();
        }
    }

    /**
     * Serves one KV v1 secret until the test stops it.
     */
    private static HttpServer startFakeVault() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/v1/secret/config", VaultDevUIFailedRefreshTest::respondWithSecret);
            server.start();
            return server;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void respondWithSecret(HttpExchange exchange) throws IOException {
        byte[] body = "{\"lease_duration\":2764800,\"data\":{\"greeting\":\"hello\"}}".getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    @Path("/greeting")
    public static class GreetingResource {

        @GET
        public String greeting() {
            return ConfigProvider.getConfig().getOptionalValue("greeting", String.class).orElse("missing");
        }
    }
}

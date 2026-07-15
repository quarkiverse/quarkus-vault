package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;

/**
 * Verifies that {@code quarkus.vault.renew-grace-period} is honored with approle authentication
 * (https://github.com/quarkiverse/quarkus-vault/issues/328).
 * <p>
 * A minimal in-process Vault stub issues approle login tokens with a TTL of 60s, and
 * {@code quarkus.vault.renew-grace-period} is set to 59s. The first Vault access logs in; a second
 * access 2s later finds a cached token expiring within the configured grace period, and must
 * therefore extend it (renew-self). If the configured grace period is ignored in favor of the
 * client's hard-coded 30s default, no renewal happens (the token still has ~58s to live) and this
 * test fails.
 * <p>
 * Note: the test class and the test profile run in different classloaders, each with its own copy
 * of the stub's statics, so the test reads the stub's counters over HTTP instead of accessing them
 * directly.
 */
@QuarkusTest
@TestProfile(AppRoleRenewGracePeriodTest.Profile.class)
public class AppRoleRenewGracePeriodTest {

    private static final int TOKEN_TTL_SECONDS = 60;
    private static final String RENEW_GRACE_PERIOD = "59s";

    @Inject
    VaultKVSecretEngine kv;

    @ConfigProperty(name = "quarkus.vault.url")
    String vaultUrl;

    @Test
    public void tokenShouldBeRenewedWithinConfiguredGracePeriod() throws Exception {

        // first access: triggers the approle login
        assertEquals("hello", kv.readSecret("foo").get("greeting"));
        assertEquals("logins=1;renewals=0", counters(),
                "first access should have logged in, without extending the freshly created token");

        Thread.sleep(2_000);

        // second access: the token expires in ~58s, within the configured 59s grace period,
        // so it must be extended (renew-self), without a new login
        assertEquals("hello", kv.readSecret("foo").get("greeting"));
        assertEquals("logins=1;renewals=1", counters(),
                "the login token expires within the configured quarkus.vault.renew-grace-period="
                        + RENEW_GRACE_PERIOD + ", so the second access should have extended it (renew-self);"
                        + " if no renewal happened, the configured grace period was ignored and the hard-coded"
                        + " 30s default (VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD) was used instead:"
                        + " https://github.com/quarkiverse/quarkus-vault/issues/328");
    }

    private String counters() throws IOException, InterruptedException {
        return HttpClient.newHttpClient()
                .send(HttpRequest.newBuilder(URI.create(vaultUrl + "/stub/counters")).build(),
                        HttpResponse.BodyHandlers.ofString())
                .body();
    }

    @AfterAll
    public static void stopStub() {
        VaultStub.stop();
    }

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                    "quarkus.vault.url", VaultStub.start(),
                    "quarkus.vault.renew-grace-period", RENEW_GRACE_PERIOD,
                    "quarkus.vault.authentication.app-role.role-id", "test-role-id",
                    "quarkus.vault.authentication.app-role.secret-id", "test-secret-id",
                    "quarkus.vault.devservices.enabled", "false");
        }
    }

    /**
     * A minimal Vault HTTP API stub supporting approle login, token renew-self and KV v2 read,
     * counting login and renew-self calls (exposed on /stub/counters).
     */
    static class VaultStub {

        static final AtomicInteger logins = new AtomicInteger();
        static final AtomicInteger renewals = new AtomicInteger();

        static volatile HttpServer server;

        static synchronized String start() {
            if (server == null) {
                try {
                    server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                server.createContext("/", VaultStub::handle);
                server.start();
            }
            return "http://localhost:" + server.getAddress().getPort();
        }

        static synchronized void stop() {
            if (server != null) {
                server.stop(0);
                server = null;
            }
        }

        static void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/v1/auth/approle/login")) {
                logins.incrementAndGet();
                respond(exchange, 200, authResponse("test-token-" + logins.get()));
            } else if (path.equals("/v1/auth/token/renew-self")) {
                renewals.incrementAndGet();
                respond(exchange, 200, authResponse(exchange.getRequestHeaders().getFirst("X-Vault-Token")));
            } else if (path.startsWith("/v1/secret/data/")) {
                respond(exchange, 200, """
                        {
                          "data": {
                            "data": {"greeting": "hello"},
                            "metadata": {"created_time": "2024-01-01T00:00:00Z", "version": 1}
                          }
                        }
                        """);
            } else if (path.equals("/stub/counters")) {
                respond(exchange, 200, "logins=" + logins.get() + ";renewals=" + renewals.get());
            } else {
                respond(exchange, 404, "{\"errors\":[\"unexpected path: " + path + "\"]}");
            }
        }

        static String authResponse(String clientToken) {
            return """
                    {
                      "auth": {
                        "client_token": "%s",
                        "accessor": "accessor-1",
                        "policies": ["default"],
                        "metadata": {},
                        "lease_duration": %d,
                        "renewable": true,
                        "num_uses": 0
                      }
                    }
                    """.formatted(clientToken, TOKEN_TTL_SECONDS);
        }

        static void respond(HttpExchange exchange, int status, String body) throws IOException {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }
    }
}

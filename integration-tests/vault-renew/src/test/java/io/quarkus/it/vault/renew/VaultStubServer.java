package io.quarkus.it.vault.renew;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * A minimal in-process stub of the Vault HTTP API, sufficient for the quarkus-vault client to
 * log in (approle or userpass, chosen with the {@code auth} init arg), renew its token
 * (renew-self) and read a KV v2 secret.
 * <p>
 * It records the timestamps of login and renew-self calls so tests can verify <b>when</b> the
 * extension decides to renew the login token. It runs in the test JVM; the application under test
 * (a separate process in integration tests) connects to it over localhost.
 */
public class VaultStubServer implements QuarkusTestResourceLifecycleManager {

    /** TTL (lease duration) returned for every login / renew-self, in seconds. */
    public static final int TOKEN_TTL_SECONDS = 60;

    /** Value of quarkus.vault.renew-grace-period. */
    public static final String RENEW_GRACE_PERIOD = "40s";

    private static final AtomicInteger TOKEN_COUNTER = new AtomicInteger();

    public static final List<Instant> LOGINS = new CopyOnWriteArrayList<>();
    public static final List<Instant> RENEWALS = new CopyOnWriteArrayList<>();

    private String auth;
    private HttpServer server;

    @Override
    public void init(Map<String, String> initArgs) {
        auth = initArgs.getOrDefault("auth", "approle");
    }

    @Override
    public Map<String, String> start() {
        LOGINS.clear();
        RENEWALS.clear();
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        server.createContext("/", this::handle);
        server.start();

        Map<String, String> config = new HashMap<>();
        config.put("quarkus.vault.url", "http://localhost:" + server.getAddress().getPort());
        config.put("quarkus.vault.renew-grace-period", RENEW_GRACE_PERIOD);
        if ("approle".equals(auth)) {
            config.put("quarkus.vault.authentication.app-role.role-id", "test-role-id");
            config.put("quarkus.vault.authentication.app-role.secret-id", "test-secret-id");
        } else {
            config.put("quarkus.vault.authentication.userpass.username", "bob");
            config.put("quarkus.vault.authentication.userpass.password", "sinclair");
        }
        return config;
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();

        if (path.equals("/v1/auth/approle/login") || path.startsWith("/v1/auth/userpass/login/")) {
            LOGINS.add(Instant.now());
            respond(exchange, 200, authResponse("test-token-" + TOKEN_COUNTER.incrementAndGet()));
        } else if (path.equals("/v1/auth/token/renew-self")) {
            RENEWALS.add(Instant.now());
            String token = exchange.getRequestHeaders().getFirst("X-Vault-Token");
            respond(exchange, 200, authResponse(token));
        } else if (path.startsWith("/v1/secret/data/")) {
            respond(exchange, 200, """
                    {
                      "request_id": "r1",
                      "lease_id": "",
                      "renewable": false,
                      "lease_duration": 0,
                      "data": {
                        "data": {"greeting": "hello"},
                        "metadata": {
                          "created_time": "2024-01-01T00:00:00Z",
                          "deletion_time": "",
                          "destroyed": false,
                          "version": 1
                        }
                      },
                      "wrap_info": null,
                      "warnings": null,
                      "auth": null
                    }
                    """);
        } else {
            respond(exchange, 404, "{\"errors\":[\"unexpected path: " + path + "\"]}");
        }
    }

    private static String authResponse(String clientToken) {
        return """
                {
                  "request_id": "r1",
                  "lease_id": "",
                  "renewable": false,
                  "lease_duration": 0,
                  "data": null,
                  "wrap_info": null,
                  "warnings": null,
                  "auth": {
                    "client_token": "%s",
                    "accessor": "accessor-1",
                    "policies": ["default"],
                    "token_policies": ["default"],
                    "metadata": {},
                    "lease_duration": %d,
                    "renewable": true,
                    "entity_id": "",
                    "token_type": "service",
                    "orphan": true,
                    "num_uses": 0
                  }
                }
                """.formatted(clientToken, TOKEN_TTL_SECONDS);
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}

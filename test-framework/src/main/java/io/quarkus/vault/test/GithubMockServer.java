package io.quarkus.vault.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.testcontainers.Testcontainers;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A minimal GitHub API stub implementing the endpoints used by Vault's github auth method:
 * user lookup, user organizations, user teams and organization lookup. Vault is pointed at it
 * via the github auth method's {@code base_url} configuration property, using the
 * {@code host.testcontainers.internal} hostname from inside the Vault container.
 */
public class GithubMockServer implements AutoCloseable {

    public static final String USERNAME = "bob";
    public static final long USER_ID = 123;
    public static final String ORGANIZATION = "testorg";
    public static final long ORGANIZATION_ID = 456;
    public static final String TEAM_NAME = "Dev Team";
    public static final String TEAM_SLUG = "dev-team";

    private static final String USER = String.format("{\"login\": \"%s\", \"id\": %d}", USERNAME, USER_ID);
    private static final String ORG = String.format("{\"login\": \"%s\", \"id\": %d}", ORGANIZATION, ORGANIZATION_ID);
    private static final String USER_ORGS = "[" + ORG + "]";
    private static final String USER_TEAMS = String.format("[{\"name\": \"%s\", \"slug\": \"%s\", \"organization\": %s}]",
            TEAM_NAME, TEAM_SLUG, ORG);

    private final HttpServer server;
    private final String validToken;

    public GithubMockServer(String validToken) {
        this.validToken = validToken;
        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/user", exchange -> respondAuthenticated(exchange, USER));
        server.createContext("/user/orgs", exchange -> respondAuthenticated(exchange, USER_ORGS));
        server.createContext("/user/teams", exchange -> respondAuthenticated(exchange, USER_TEAMS));
        // left unauthenticated: Vault resolves the organization id here at config time, before any user token exists
        server.createContext("/orgs/" + ORGANIZATION, exchange -> respond(exchange, 200, ORG));
    }

    public void start() {
        server.start();
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
    }

    /**
     * Base URL of the mock GitHub API, as seen from inside a docker container started with
     * {@code withAccessToHost(true)}.
     */
    public String getBaseUrl() {
        return "http://host.testcontainers.internal:" + server.getAddress().getPort() + "/";
    }

    @Override
    public void close() {
        server.stop(0);
    }

    /**
     * Responds with the given body only if the request carries the valid token, accepting both
     * authorization schemes supported by GitHub: {@code Bearer <token>} and {@code token <token>}.
     */
    private void respondAuthenticated(HttpExchange exchange, String body) throws IOException {
        var header = exchange.getRequestHeaders().getFirst("Authorization");
        var token = header == null ? null : header.replaceFirst("(?i)^(Bearer|token)\\s+", "");
        if (validToken.equals(token)) {
            respond(exchange, 200, body);
        } else {
            respond(exchange, 401, "{\"message\": \"Bad credentials\"}");
        }
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}

package io.quarkus.vault.client.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * A minimal GitHub API stub implementing the endpoints used by Vault's github auth method:
 * user lookup, user organizations, user teams and organization lookup. Vault is pointed at it
 * via the github auth method's {@code base_url} configuration property, using the
 * {@code host.docker.internal} hostname from inside the Vault container.
 */
public class GithubMockServer implements AutoCloseable {

    public static final String USERNAME = "bob";
    public static final long USER_ID = 123;
    public static final String ORGANIZATION = "testorg";
    public static final long ORGANIZATION_ID = 456;
    public static final String TEAM_NAME = "Dev Team";
    public static final String TEAM_SLUG = "dev-team";

    private static final String USER = """
            {"login": "%s", "id": %d}""".formatted(USERNAME, USER_ID);
    private static final String ORG = """
            {"login": "%s", "id": %d}""".formatted(ORGANIZATION, ORGANIZATION_ID);
    private static final String USER_ORGS = "[" + ORG + "]";
    private static final String USER_TEAMS = """
            [{"name": "%s", "slug": "%s", "organization": %s}]""".formatted(TEAM_NAME, TEAM_SLUG, ORG);

    private final HttpServer server;

    public GithubMockServer() {
        try {
            server = HttpServer.create(new InetSocketAddress(0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/user", exchange -> respond(exchange, USER));
        server.createContext("/user/orgs", exchange -> respond(exchange, USER_ORGS));
        server.createContext("/user/teams", exchange -> respond(exchange, USER_TEAMS));
        server.createContext("/orgs/" + ORGANIZATION, exchange -> respond(exchange, ORG));
    }

    public void start() {
        server.start();
        org.testcontainers.Testcontainers.exposeHostPorts(server.getAddress().getPort());
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

    private static void respond(HttpExchange exchange, String body) throws IOException {
        var bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, bytes.length);
        try (var out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}

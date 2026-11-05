package io.quarkus.vault.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

final class VaultDevMcpTestClient {

    private VaultDevMcpTestClient() {
    }

    static JsonNode configSources(URI uri) throws Exception {
        JsonNode response = callTool(uri, "getConfigSourcesForMcp");
        assertFalse(response.has("error"), response.toString());
        return new ObjectMapper().readTree(response.get("result").get("content").get(0).get("text").asString());
    }

    static JsonNode callTool(URI uri, String method) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(Map.of("jsonrpc", "2.0", "id", 1, "method", "tools/call",
                "params", Map.of("name", "quarkus-vault_" + method, "arguments", Map.of())));
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json, text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build();
        try (HttpClient client = HttpClient.newHttpClient()) {
            var response = client.send(request, BodyHandlers.ofString());
            assertEquals(200, response.statusCode(), response.body());
            return mapper.readTree(response.body());
        }
    }
}

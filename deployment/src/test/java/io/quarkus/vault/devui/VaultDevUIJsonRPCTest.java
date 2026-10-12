package io.quarkus.vault.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import io.quarkus.test.common.http.TestHTTPResource;
import tools.jackson.databind.JsonNode;

@Tag("devmode")
@QuarkusTestResource(value = VaultDevUIJsonRPCTest.VaultConfiguration.class, restrictToAnnotatedClass = true)
public class VaultDevUIJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withEmptyApplication();

    @TestHTTPResource("/q/dev-ui/")
    URI devUiUrl;

    @TestHTTPResource("/q/dev-mcp")
    URI devMcpUrl;

    public VaultDevUIJsonRPCTest() {
        super("quarkus-vault");
    }

    @Test
    public void serverVersionIsReportedFromDevServicesVault() throws Exception {
        String version = executeJsonRPCMethod("getServerVersion").asString();

        assertFalse(version.isBlank(), version);
        assertNotEquals("unreachable", version);
        assertTrue(version.matches("\\d+\\.\\d+.*"), version);
    }

    @Test
    public void vaultUiUrlIsResolvedFromDevServicesVault() throws Exception {
        JsonNode url = executeJsonRPCMethod("getVaultUiUrl");

        assertTrue(url.asString().startsWith("http://"), url.asString());
        assertTrue(url.asString().endsWith("/ui"), url.asString());
    }

    @Test
    public void configSourcesReportPropertyNamesPerKvPath() throws Exception {
        JsonNode snapshot = executeJsonRPCMethod("getConfigSources");
        assertFalse(snapshot.toString().contains("hello"), snapshot.toString());

        JsonNode path = null;
        for (JsonNode candidate : snapshot.get("paths")) {
            if ("devui/config".equals(candidate.get("path").asString())) {
                path = candidate;
            }
        }
        assertNotNull(path, snapshot.toString());
        JsonNode error = path.get("error");
        assertTrue(error == null || error.isNull(), path.toString());
        assertEquals(1, path.get("propertyNames").size());
        assertEquals("greeting", path.get("propertyNames").get(0).asString());
        assertEquals("1", executeJsonRPCMethod("getConfigSourcesLabel").asString());
    }

    @Test
    public void mcpExposureRequiresProjectOptInWithoutHidingDevUI() throws Exception {
        JsonNode snapshot = executeJsonRPCMethod("getConfigSources");
        assertEquals("greeting", snapshot.get("paths").get(0).get("propertyNames").get(0).asString());
        assertEquals("1", executeJsonRPCMethod("getConfigSourcesLabel").asString());
        assertTrue(VaultDevMcpTestClient.configSources(devMcpUrl).isNull());

        JsonNode bypass = VaultDevMcpTestClient.callTool(devMcpUrl, "getConfigSources");
        assertEquals(-32601, bypass.get("error").get("code").asInt(), bypass.toString());
        assertFalse(bypass.toString().contains("greeting"));
        assertFalse(bypass.toString().contains("devui/config"));

        setMetadataExposure(true);
        try {
            JsonNode exposed = VaultDevMcpTestClient.configSources(devMcpUrl);
            assertEquals(executeJsonRPCMethod("getConfigSources"), exposed);
            assertFalse(exposed.toString().contains("hello"));
        } finally {
            setMetadataExposure(false);
        }

        assertEquals("1", executeJsonRPCMethod("getConfigSourcesLabel").asString());
        assertEquals("greeting", executeJsonRPCMethod("getConfigSources")
                .get("paths").get(0).get("propertyNames").get(0).asString());
        assertTrue(VaultDevMcpTestClient.configSources(devMcpUrl).isNull());
    }

    private void setMetadataExposure(boolean enabled) throws Exception {
        // Close test WebSockets before restarting the application.
        super.afterEach();
        TEST.modifyResourceFile("application.properties", properties -> properties
                .replaceAll("(?m)^quarkus\\.vault\\.dev-ui\\.expose-config-source-metadata-to-mcp=.*(?:\\R|$)", "")
                + "\nquarkus.vault.dev-ui.expose-config-source-metadata-to-mcp=" + enabled + "\n");
        try (HttpClient client = HttpClient.newHttpClient()) {
            assertEquals(200, client.send(HttpRequest.newBuilder(devUiUrl).GET().build(), BodyHandlers.discarding())
                    .statusCode());
        }
    }

    public static class VaultConfiguration implements QuarkusTestResourceLifecycleManager {
        @Override
        public Map<String, String> start() {
            return Map.of(
                    "quarkus.vault.devservices.init-commands", "kv put secret/devui/config greeting=hello",
                    "quarkus.dev-mcp.enabled", "true",
                    "quarkus.vault.secret-config-kv-path", "devui/config");
        }

        @Override
        public void stop() {
        }
    }
}

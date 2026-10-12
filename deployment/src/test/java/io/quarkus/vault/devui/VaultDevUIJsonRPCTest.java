package io.quarkus.vault.devui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devui.tests.DevUIJsonRPCTest;
import io.quarkus.test.QuarkusDevModeTest;
import tools.jackson.databind.JsonNode;

@Tag("devmode")
public class VaultDevUIJsonRPCTest extends DevUIJsonRPCTest {

    @RegisterExtension
    static final QuarkusDevModeTest TEST = new QuarkusDevModeTest()
            .withApplicationRoot(jar -> jar.addAsResource(new StringAsset("""
                    quarkus.vault.devservices.init-commands=kv put secret/devui/config greeting=hello
                    quarkus.vault.secret-config-kv-path=devui/config
                    """), "application.properties"));

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
}

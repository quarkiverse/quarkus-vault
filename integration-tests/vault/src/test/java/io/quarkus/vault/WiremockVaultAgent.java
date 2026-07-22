package io.quarkus.vault;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.absent;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

/**
 * Simulates a Vault Agent listener with auto-auth enabled: the agent authenticates on behalf of the application
 * and injects the Vault token into proxied requests itself, so requests coming from the application must not
 * carry any X-Vault-Token header. The stub only matches requests where the header is absent.
 */
public class WiremockVaultAgent implements QuarkusTestResourceLifecycleManager {

    private WireMockServer server;

    @Override
    public Map<String, String> start() {

        server = new WireMockServer(wireMockConfig().dynamicPort());
        server.start();

        server.stubFor(get(urlEqualTo("/v1/secret/foo"))
                .withHeader("X-Vault-Token", absent())
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"request_id\":\"bf5245f4-f194-2b13-80b7-6cad145b8135\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"wrap_info\":null,\"warnings\":null,\"auth\":null,"
                                        + "\"data\":{\"hello\":\"world\"}}")));

        return Map.of("vault-agent-test.url", "http://localhost:" + server.port());
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop();
        }
    }
}

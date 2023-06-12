package io.quarkus.vault;

import java.util.Collections;
import java.util.Map;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

public class WiremockProxy implements QuarkusTestResourceLifecycleManager {

    public static final String PROXY_HOST = "localhost";
    public static final int PROXY_PORT = 3128;

    private WireMockServer proxyServer;

    private WireMockServer vaultServer;

    @Override
    public Map<String, String> start() {

        proxyServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(PROXY_PORT)
                        .enableBrowserProxying(true)
                        .bindAddress(PROXY_HOST)
                        .stubRequestLoggingDisabled(false)
                        .trustAllProxyTargets(true));

        proxyServer.start();

        vaultServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig()
                        .port(8282)
                        .stubRequestLoggingDisabled(false));
        vaultServer.start();

        vaultServer.stubFor(get(urlEqualTo("/v1/secret/foo"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(
                                "{\"request_id\":\"bf5245f4-f194-2b13-80b7-6cad145b8135\",\"lease_id\":\"\",\"renewable\":false,\"lease_duration\":2764800,\"wrap_info\":null,\"warnings\":null,\"auth\":null,"
                                        + "\"data\":{\"hello\":\"world\"}}")));
        return Collections.emptyMap();
    }

    @Override
    public void stop() {
        if (proxyServer != null) {
            proxyServer.stop();
            proxyServer = null;
        }
        if (vaultServer != null) {
            vaultServer.stop();
            vaultServer = null;
        }
    }
}

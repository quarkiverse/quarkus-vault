package io.quarkus.vault;

import static io.quarkus.vault.WiremockProxy.PROXY_HOST;
import static io.quarkus.vault.WiremockProxy.PROXY_PORT;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Inject;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(WiremockProxy.class)
class VaultProxyITCase {

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @ConfigProperty(name = "quarkus.vault.proxy-host")
    String host;

    @ConfigProperty(name = "quarkus.vault.proxy-port")
    Integer port;

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-proxy.properties", "application.properties"));

    @Test
    void proxyConfig() {
        assertEquals(PROXY_HOST, host);
        assertEquals(PROXY_PORT, port);
        assertEquals("{hello=world}", kvSecretEngine.readSecret("foo").toString());
        assertEquals("true", kvSecretEngine.readSecret("withBoolean").get("isTrue"));
        assertNull(kvSecretEngine.readSecret("withNull").get("foo"));
    }

}

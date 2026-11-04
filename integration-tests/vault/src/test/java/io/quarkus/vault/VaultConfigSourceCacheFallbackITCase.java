package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URL;
import java.util.stream.StreamSupport;

import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.config.spi.ConfigSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import com.github.tomakehurst.wiremock.client.WireMock;

import io.quarkus.test.QuarkusExtensionTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * When a refresh fails with an IO error, the config source should keep serving the last known values for the cache
 * period, not only for the call that hit the error.
 */
@QuarkusTestResource(WiremockVaultConfigSource.class)
public class VaultConfigSourceCacheFallbackITCase {

    private static final long CACHE_PERIOD_MILLIS = 1000;

    @RegisterExtension
    static final QuarkusExtensionTest config = new QuarkusExtensionTest()
            .withEmptyApplication()
            .overrideConfigKey("quarkus.vault.url", "${vault-config-source-test.url}")
            .overrideConfigKey("quarkus.vault.authentication.client-token", "123")
            .overrideConfigKey("quarkus.vault.kv-secret-engine-version", "1")
            .overrideConfigKey("quarkus.vault.secret-config-kv-path", "config")
            .overrideConfigKey("quarkus.vault.secret-config-cache-period", CACHE_PERIOD_MILLIS + "ms")
            .overrideConfigKey("quarkus.vault.devservices.enabled", "false");

    @Test
    public void lastKnownValueIsKeptForTheCachePeriodAfterAnIOError() throws Exception {
        ConfigSource source = vaultConfigSource();
        assertEquals("hello", source.getValue("greeting"));

        // vault becomes unreachable and the cache expires: the next lookup triggers a refresh that fails
        // (shutdown goes through the admin API, because this test class and the resource live in different class loaders)
        URL vaultUrl = ConfigProvider.getConfig().getValue("quarkus.vault.url", URL.class);
        new WireMock(vaultUrl.getHost(), vaultUrl.getPort()).shutdown();
        Thread.sleep(CACHE_PERIOD_MILLIS + 500);

        assertEquals("hello", source.getValue("greeting"), "call that hits the refresh error");
        assertEquals("hello", source.getValue("greeting"), "next call within the cache period");
    }

    private static ConfigSource vaultConfigSource() {
        return StreamSupport.stream(ConfigProvider.getConfig().getConfigSources().spliterator(), false)
                .filter(source -> VaultRuntimeConfig.NAME.equals(source.getName()))
                .findFirst()
                .orElseThrow();
    }
}

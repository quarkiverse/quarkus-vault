package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;
import static io.vertx.core.spi.resolver.ResolverProvider.DISABLE_DNS_RESOLVER_PROP_NAME;

import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.vertx.core.VertxOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
@Private
public class PrivateVertxVaultClient extends VertxVaultClient {

    private AtomicReference<Vertx> vertx = new AtomicReference<>();
    private AtomicReference<WebClient> webClient = new AtomicReference<>();
    private final TlsConfig tlsConfig;
    private final VaultConfigHolder vaultConfigHolder;

    public PrivateVertxVaultClient(VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {
        super(vaultConfigHolder.getVaultBootstrapConfig().url.orElseThrow(() -> new VaultException("no vault url provided")),
                vaultConfigHolder.getVaultBootstrapConfig().enterprise.namespace,
                vaultConfigHolder.getVaultBootstrapConfig().readTimeout);
        this.vaultConfigHolder = vaultConfigHolder;
        this.tlsConfig = tlsConfig;
    }

    @Override
    protected WebClient getWebClient() {
        WebClient webClient = this.webClient.get();
        if (webClient == null) {
            webClient = createHttpClient(getVertx(), vaultConfigHolder.getVaultBootstrapConfig(), tlsConfig);
            if (!this.webClient.compareAndSet(null, webClient)) {
                webClient.close();
                return this.webClient.get();
            }
        }
        return webClient;
    }

    private Vertx getVertx() {
        Vertx vertx = this.vertx.get();
        if (vertx == null) {
            vertx = createVertxInstance();
            if (!this.vertx.compareAndSet(null, vertx)) {
                vertx.close().await().indefinitely();
                return this.vertx.get();
            }
        }
        return vertx;
    }

    private Vertx createVertxInstance() {
        // We must disable the async DNS resolver as it can cause issues when resolving the Vault instance.
        // This is done using the DISABLE_DNS_RESOLVER_PROP_NAME system property.
        // The DNS resolver used by vert.x is configured during the (synchronous) initialization.
        // So, we just need to disable the async resolver around the Vert.x instance creation.
        String originalValue = System.getProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
        Vertx vertx;
        try {
            System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, "true");
            vertx = Vertx.vertx(new VertxOptions());
        } finally {
            // Restore the original value
            if (originalValue == null) {
                System.clearProperty(DISABLE_DNS_RESOLVER_PROP_NAME);
            } else {
                System.setProperty(DISABLE_DNS_RESOLVER_PROP_NAME, originalValue);
            }
        }
        return vertx;
    }

    @PreDestroy
    @Override
    public void close() {
        try {
            WebClient webClient = this.webClient.getAndSet(null);
            if (webClient != null) {
                webClient.close();
            }
        } finally {
            Vertx vertx = this.vertx.getAndSet(null);
            if (vertx != null) {
                vertx.closeAndAwait();
            }
        }
    }
}

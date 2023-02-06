package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;

import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vertx.runtime.VertxRecorder;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
@Shared
public class SharedVertxVaultClient extends VertxVaultClient {

    @Produces
    @Dependent
    public static VertxVaultClient createSharedVaultClient() {
        ArcContainer container = Arc.container();
        VaultConfigHolder vaultConfigHolder = container.select(VaultConfigHolder.class).get();
        TlsConfig tlsConfig = container.select(TlsConfig.class).get();
        io.vertx.core.Vertx vertx = VertxRecorder.getVertx();
        if (vertx == null) {
            return new PrivateVertxVaultClient(vaultConfigHolder, tlsConfig);
        } else {
            return new SharedVertxVaultClient(Vertx.newInstance(vertx), vaultConfigHolder, tlsConfig);
        }

    }

    private final AtomicReference<WebClient> webClient = new AtomicReference<>();

    public SharedVertxVaultClient(Vertx vertx, VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {
        super(vaultConfigHolder.getVaultBootstrapConfig().url.orElseThrow(() -> new VaultException("no vault url provided")),
                vaultConfigHolder.getVaultBootstrapConfig().enterprise.namespace,
                vaultConfigHolder.getVaultBootstrapConfig().readTimeout);
        this.webClient.set(createHttpClient(vertx, vaultConfigHolder.getVaultBootstrapConfig(), tlsConfig));
    }

    @Override
    protected WebClient getWebClient() {
        return webClient.get();
    }

    @PreDestroy
    @Override
    public void close() {
        WebClient webClient = this.webClient.getAndSet(null);
        if (webClient != null) {
            webClient.close();
        }
    }

}

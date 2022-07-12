package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.PreDestroy;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.Produces;
import javax.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
@Shared
public class SharedVertxVaultClient extends VertxVaultClient {

    @Produces
    @Dependent
    public static VertxVaultClient createSharedVaultClient(Instance<Vertx> vertx) {
        Annotation clientType;
        if (vertx.isResolvable()) {
            clientType = Shared.Literal.INSTANCE;
        } else {
            clientType = Private.Literal.INSTANCE;
        }
        return Arc.container().select(VertxVaultClient.class, clientType).get();
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

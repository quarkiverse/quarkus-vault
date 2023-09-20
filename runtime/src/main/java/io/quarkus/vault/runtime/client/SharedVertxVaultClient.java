package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;

import java.lang.annotation.Annotation;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vertx.runtime.VertxEventBusConsumerRecorder;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
@Shared
public class SharedVertxVaultClient extends VertxVaultClient {

    @Produces
    @Dependent
    public static VertxVaultClient createSharedVaultClient() {
        Annotation clientType;
        if (VertxEventBusConsumerRecorder.getVertx() != null) {
            clientType = Shared.Literal.INSTANCE;
        } else {
            clientType = Private.Literal.INSTANCE;
        }
        return Arc.container().select(VertxVaultClient.class, clientType).get();
    }

    private final AtomicReference<WebClient> webClient = new AtomicReference<>();

    public SharedVertxVaultClient(VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {
        super(vaultConfigHolder.getVaultRuntimeConfig().url().orElseThrow(() -> new VaultException("no vault url provided")),
                vaultConfigHolder.getVaultRuntimeConfig().enterprise().namespace(),
                vaultConfigHolder.getVaultRuntimeConfig().readTimeout());
        Vertx vertx = Vertx.newInstance(VertxEventBusConsumerRecorder.getVertx());
        this.webClient.set(createHttpClient(vertx, vaultConfigHolder.getVaultRuntimeConfig(), tlsConfig));
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

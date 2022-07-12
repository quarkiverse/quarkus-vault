package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.client.MutinyVertxClientFactory.createHttpClient;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
public class SharedVertxVaultClient extends VertxVaultClient {

    private final WebClient webClient;

    public SharedVertxVaultClient(Vertx vertx, VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {
        super(vaultConfigHolder.getVaultBootstrapConfig().url.orElseThrow(() -> new VaultException("no vault url provided")),
                vaultConfigHolder.getVaultBootstrapConfig().enterprise.namespace,
                vaultConfigHolder.getVaultBootstrapConfig().readTimeout);
        this.webClient = createHttpClient(vertx, vaultConfigHolder.getVaultBootstrapConfig(), tlsConfig);
    }

    @Override
    protected WebClient getWebClient() {
        return webClient;
    }

    @PreDestroy
    @Override
    public Uni<Void> close() {
        try {
            if (webClient != null) {
                webClient.close();
            }
        } catch (Throwable ignored) {
        } finally {
            return Uni.createFrom().voidItem();
        }
    }

}

package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;

import org.jboss.logging.Logger;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

public class MutinyVertxClientFactory {

    private static final Logger log = Logger.getLogger(MutinyVertxClientFactory.class.getName());

    public static WebClient createHttpClient(Vertx vertx, VaultRuntimeConfig vaultRuntimeConfig, TlsConfig tlsConfig) {

        WebClientOptions options = new WebClientOptions()
                .setConnectTimeout((int) vaultRuntimeConfig.connectTimeout().toMillis())
                .setIdleTimeout((int) vaultRuntimeConfig.readTimeout().getSeconds() * 2);

        if (vaultRuntimeConfig.proxyHost().isPresent()) {
            options.setProxyOptions(
                    new ProxyOptions()
                            .setHost(vaultRuntimeConfig.proxyHost().get())
                            .setPort(vaultRuntimeConfig.proxyPort()));
        }

        if (vaultRuntimeConfig.nonProxyHosts().isPresent()) {
            options.setNonProxyHosts(vaultRuntimeConfig.nonProxyHosts().get());
        }

        boolean trustAll = vaultRuntimeConfig.tls().skipVerify().orElseGet(() -> tlsConfig.trustAll);
        if (trustAll) {
            skipVerify(options);
        } else if (vaultRuntimeConfig.tls().caCert().isPresent()) {
            cacert(options, vaultRuntimeConfig.tls().caCert().get());
        } else if (vaultRuntimeConfig.getAuthenticationType() == KUBERNETES
                && vaultRuntimeConfig.tls().useKubernetesCaCert()) {
            cacert(options, KUBERNETES_CACERT);
        }

        return WebClient.create(vertx, options);
    }

    private static void cacert(WebClientOptions options, String cacert) {
        log.debug("configure tls with " + cacert);
        options.setTrustOptions(new PemTrustOptions().addCertPath(cacert));
    }

    private static void skipVerify(WebClientOptions options) {
        log.debug("configure tls with skip-verify");
        options.setTrustAll(true);
        options.setVerifyHost(false);
    }
}

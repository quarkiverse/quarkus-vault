package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.vertx.core.Vertx;
import io.vertx.core.net.PemTrustOptions;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;

public class MutinyVertxClientFactory {

    private static final Logger log = Logger.getLogger(MutinyVertxClientFactory.class.getName());

    public static WebClient createHttpClient(Vertx vertx, VaultRuntimeConfig vaultRuntimeConfig) {

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

        boolean globalTrustAll = ConfigProvider.getConfig().getOptionalValue("quarkus.tls.trust-all", Boolean.class)
                .orElse(false);
        boolean trustAll = vaultRuntimeConfig.tls().skipVerify().orElse(globalTrustAll);
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

package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.net.ssl.*;

import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.pki.X509Parsing;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class JDKClientFactory {

    public static HttpClient createHttpClient(VaultRuntimeConfig vaultRuntimeConfig, boolean globalTrustAll) {

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(vaultRuntimeConfig.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (vaultRuntimeConfig.proxyHost().isPresent()) {
            var proxyAddress = new InetSocketAddress(vaultRuntimeConfig.proxyHost().get(), vaultRuntimeConfig.proxyPort());
            var nonProxyHosts = vaultRuntimeConfig.nonProxyHosts().orElse(List.of());
            builder = builder.proxy(new NonProxyHostsSupportingProxySelector(proxyAddress, nonProxyHosts));
        }

        SSLContext sslContext = createSSLContext(vaultRuntimeConfig, globalTrustAll);
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        return builder.build();
    }

    private static SSLContext createSSLContext(VaultRuntimeConfig config, boolean globalTrustAll) {
        var tlsConfig = config.tls();
        boolean trustAll = tlsConfig.skipVerify().orElseGet(() -> globalTrustAll);
        if (trustAll) {
            return skipVerify();
        } else if (tlsConfig.caCert().isPresent()) {
            return buildSslContextFromPem(tlsConfig.caCert().get());
        } else if (config.getAuthenticationType() == KUBERNETES && tlsConfig.useKubernetesCaCert()) {
            return buildSslContextFromPem(KUBERNETES_CACERT);
        } else {
            return null;
        }
    }

    private static SSLContext buildSslContextFromPem(String file) throws VaultException {
        try {

            final var certificate = X509Parsing.parsePEMCertificate(Files.readString(Paths.get(file), UTF_8));

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());

            // Build a truststore
            final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            keyStore.setCertificateEntry("caCert", certificate);
            trustManagerFactory.init(keyStore);
            TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();

            final SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagers, null);
            return sslContext;
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new VaultException(e);
        }
    }

    private static SSLContext skipVerify() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new TrustAllManager() }, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new VaultException(e);
        }
    }

    static class TrustAllManager extends X509ExtendedTrustManager {
        @Override
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return new java.security.cert.X509Certificate[0];
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
            // no-op
        }

        @Override
        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType)
                throws CertificateException {
            // no-op
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            // no-op
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, Socket socket) throws CertificateException {
            // no-op
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            // no-op
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType, SSLEngine engine) throws CertificateException {
            // no-op
        }
    }

    static class NonProxyHostsSupportingProxySelector extends ProxySelector {

        private final ProxySelector delegate;
        private final List<String> nonProxyHosts;

        public NonProxyHostsSupportingProxySelector(InetSocketAddress proxyAddress, List<String> nonProxyHosts) {
            this.delegate = ProxySelector.of(proxyAddress);
            this.nonProxyHosts = nonProxyHosts;
        }

        @Override
        public List<Proxy> select(URI uri) {
            if (nonProxyHosts.stream().anyMatch(uri.getHost()::matches)) {
                return List.of(Proxy.NO_PROXY);
            }
            return delegate.select(uri);
        }

        @Override
        public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            delegate.connectFailed(uri, sa, ioe);
        }
    }
}

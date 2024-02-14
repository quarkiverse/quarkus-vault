package io.quarkus.vault.runtime.client;

import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.http.HttpClient;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.*;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.pki.X509Parsing;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class JDKClientFactory {

    public static HttpClient createHttpClient(VaultRuntimeConfig vaultRuntimeConfig, TlsConfig tlsConfig) {

        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(vaultRuntimeConfig.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL);

        if (vaultRuntimeConfig.proxyHost().isPresent()) {
            // TODO: Need to support non-proxy hosts
            var proxySelector = ProxySelector
                    .of((new InetSocketAddress(vaultRuntimeConfig.proxyHost().get(), vaultRuntimeConfig.proxyPort())));
            builder = builder.proxy(proxySelector);
        }

        SSLContext sslContext = createSSLContext(vaultRuntimeConfig, tlsConfig);
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }

        return builder.build();
    }

    private static SSLContext createSSLContext(VaultRuntimeConfig config, TlsConfig globalTlsConfig) {
        var tlsConfig = config.tls();
        boolean trustAll = tlsConfig.skipVerify().orElseGet(() -> globalTlsConfig.trustAll);
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

}

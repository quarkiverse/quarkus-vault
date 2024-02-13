package io.quarkus.vault.runtime;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static io.quarkus.vault.runtime.LogConfidentialityLevel.LOW;
import static io.quarkus.vault.runtime.client.VaultClient.API_VERSION;
import static io.quarkus.vault.runtime.client.VaultClient.X_VAULT_TOKEN;
import static io.quarkus.vault.runtime.config.VaultAuthenticationType.KUBERNETES;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.KUBERNETES_CACERT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.*;

import org.eclipse.microprofile.config.ConfigProvider;
import org.jboss.logging.Logger;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import io.quarkus.vault.VaultException;
import io.quarkus.vault.runtime.client.dto.auth.*;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretJsonV1;
import io.quarkus.vault.runtime.client.dto.kv.VaultKvSecretJsonV2;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapBody;
import io.quarkus.vault.runtime.config.VaultAuthenticationConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.config.VaultTlsConfig;

public class JDKVaultClient {

    private static final Logger log = Logger.getLogger(JDKVaultClient.class);

    final private VaultRuntimeConfig config;

    final private ObjectMapper mapper;

    volatile String unwrappedToken = null;

    public JDKVaultClient(VaultRuntimeConfig config) {
        this.config = config;
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    private String getVaultToken() {
        try {
            VaultAuthenticationConfig authentication = config.authentication();
            if (authentication.isDirectClientToken()) {
                return getDirectClientToken();
            } else if (authentication.isAppRole()) {
                return loginAppRole();
            } else if (authentication.isUserpass()) {
                return loginUserPass();
            } else if (authentication.kubernetes().role().isPresent()) {
                return loginKubernetes();
            } else {
                throw new UnsupportedOperationException("invalid vault authentication method");
            }
        } catch (IOException e) {
            throw new VaultIOException(e);
        }
    }

    private String getDirectClientToken() throws IOException {
        var authentication = config.authentication();
        Optional<String> clientToken = authentication.clientToken();
        if (clientToken.isPresent()) {
            return clientToken.get();
        } else if (unwrappedToken != null) {
            return unwrappedToken;
        } else {
            var wrappingToken = authentication.clientTokenWrappingToken();
            return unwrappedToken = post("unwrap client token", "sys/wrapping/unwrap", wrappingToken, VaultUnwrapBody.EMPTY,
                    VaultTokenCreate.class).auth.clientToken;
        }
    }

    public String loginAppRole() throws IOException {
        VaultAuthenticationConfig authentication = config.authentication();
        String mountPath = authentication.appRole().authMountPath();
        String roleId = authentication.appRole().roleId().orElseThrow();
        String secretId = authentication.appRole().secretId().orElseThrow();
        VaultAppRoleAuthBody body = new VaultAppRoleAuthBody(roleId, secretId);
        return post("login with approle", mountPath + "/login", Optional.empty(), body,
                VaultAppRoleAuth.class).auth.clientToken;
    }

    public String loginUserPass() throws IOException {
        VaultAuthenticationConfig authentication = config.authentication();
        String user = authentication.userpass().username().orElseThrow();
        String password = authentication.userpass().password().orElseThrow();
        VaultUserPassAuthBody body = new VaultUserPassAuthBody(password);
        return post("login with userpass", "auth/userpass/login/" + user, Optional.empty(), body,
                VaultUserPassAuth.class).auth.clientToken;
    }

    public String loginKubernetes() throws IOException {
        VaultAuthenticationConfig authentication = config.authentication();
        String mountPath = authentication.kubernetes().authMountPath();
        String jwt = Files.readString(Paths.get(authentication.kubernetes().jwtTokenPath()), UTF_8);
        String role = authentication.kubernetes().role().orElseThrow();
        VaultKubernetesAuthBody body = new VaultKubernetesAuthBody(role, jwt);
        log.debug("authenticate with jwt at: " + authentication.kubernetes().jwtTokenPath() + " => "
                + config.logConfidentialityLevel().maskWithTolerance(jwt, LOW));
        return post("login with kubernetes", mountPath + "/login", Optional.empty(), body,
                VaultKubernetesAuth.class).auth.clientToken;
    }

    public Map<String, Object> readSecret(String secretPath) {
        int version = config.kvSecretEngineVersion();
        if (!List.of(1, 2).contains(version)) {
            throw new VaultException("invalid version " + version + " for kv secret engine");
        }
        try {
            String mount = config.kvSecretEngineMountPath();
            String path = mount + (version == 1 ? "" : "/data") + "/" + secretPath;
            HttpRequest request = newRequestBuilder()
                    .uri(getUri(path))
                    .header(X_VAULT_TOKEN, getVaultToken())
                    .GET()
                    .build();

            HttpResponse<String> response = send(request);
            if (response.statusCode() != 200) {
                throw new VaultException("unable to read path " + path + "; status code = " + response.statusCode());
            }
            if (version == 1) {
                return mapper.readValue(response.body(), VaultKvSecretJsonV1.class).data;
            } else {
                return mapper.readValue(response.body(), VaultKvSecretJsonV2.class).data.data;
            }
        } catch (IOException e) {
            throw new VaultIOException(e);
        }
    }

    private HttpResponse<String> send(HttpRequest request) throws IOException {
        try {
            return getHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new VaultException(e);
        }
    }

    private <T> T post(String description, String path, Optional<String> wrappingToken, Object body, Class<T> resultClass)
            throws IOException {
        var bodyPublisher = HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body), UTF_8);
        var builder = newRequestBuilder().uri(getUri(path)).POST(bodyPublisher);
        if (wrappingToken.isPresent()) {
            builder.header(X_VAULT_TOKEN, wrappingToken.orElseThrow());
        }
        HttpRequest request = builder.build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new VaultException("unable to " + description + "; status code = " + response.statusCode());
        }
        return mapper.readValue(response.body(), resultClass);
    }

    private URI getUri(String path) {
        try {
            String baseUrl = config.url().orElseThrow().toURI().toString();
            return new URI(baseUrl + "/" + API_VERSION + "/" + path);
        } catch (URISyntaxException e) {
            throw new VaultException(e);
        }
    }

    private HttpRequest.Builder newRequestBuilder() {
        return HttpRequest.newBuilder().timeout(config.readTimeout());
    }

    private HttpClient getHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder().connectTimeout(config.connectTimeout());
        SSLContext sslContext = createSSLContext();
        if (sslContext != null) {
            builder.sslContext(sslContext);
        }
        if (config.proxyHost().isPresent()) {
            var proxy = new InetSocketAddress(config.proxyHost().orElseThrow(), config.proxyPort());
            builder.proxy(ProxySelector.of(proxy));
        }
        return builder.build();
    }

    private SSLContext createSSLContext() {
        VaultTlsConfig tls = config.tls();
        if (isTrustAll()) {
            return skipVerify();
        } else if (tls.caCert().isPresent()) {
            return buildSslContextFromPem(tls.caCert().get());
        } else if (config.getAuthenticationType() == KUBERNETES && tls.useKubernetesCaCert()) {
            return buildSslContextFromPem(KUBERNETES_CACERT);
        } else {
            return null;
        }
    }

    private SSLContext buildSslContextFromPem(String file) throws VaultException {
        try {
            String pemUTF8 = Files.readString(Paths.get(file), UTF_8);
            final CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

            final TrustManagerFactory trustManagerFactory = TrustManagerFactory
                    .getInstance(TrustManagerFactory.getDefaultAlgorithm());
            // Convert the trusted servers PEM data into an X509Certificate
            X509Certificate certificate;
            try (final ByteArrayInputStream pem = new ByteArrayInputStream(pemUTF8.getBytes(UTF_8))) {
                certificate = (X509Certificate) certificateFactory.generateCertificate(pem);
            }
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

    private SSLContext skipVerify() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[] { new TrustAllManager() }, new SecureRandom());
            return sslContext;
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new VaultException(e);
        }
    }

    private boolean isTrustAll() {
        boolean trustAll = ConfigProvider.getConfig().getOptionalValue("quarkus.tls.trust-all", Boolean.class).orElse(false);
        boolean skipVerify = config.tls().skipVerify().orElse(false);
        return trustAll || skipVerify;
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

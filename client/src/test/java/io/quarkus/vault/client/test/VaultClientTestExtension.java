package io.quarkus.vault.client.test;

import static org.testcontainers.containers.BindMode.READ_ONLY;

import java.io.IOException;
import java.net.URL;
import java.net.http.HttpClient;
import java.security.MessageDigest;
import java.util.HexFormat;

import org.junit.jupiter.api.extension.*;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultSysPluginsTest;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;

@VaultClientTest
public class VaultClientTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final java.util.Random random = new java.util.Random();

    public static final String DEFAULT_VAULT_IMAGE_REPO = "hashicorp/vault";
    public static final String DEFAULT_VAULT_IMAGE_VER = "1.15.4";

    public static String getVaultImageVersion() {
        var version = System.getenv("VAULT_IMAGE_VER");
        return version != null ? version : DEFAULT_VAULT_IMAGE_VER;
    }

    public static String getVaultImageRepo() {
        var repo = System.getenv("VAULT_IMAGE_REPO");
        return repo != null ? repo : DEFAULT_VAULT_IMAGE_REPO;
    }

    public static String getVaultImage() {
        var tag = System.getenv("VAULT_IMAGE_TAG");
        return tag != null ? tag : getVaultImageRepo() + ":" + getVaultImageVersion();
    }

    // Generate a random path to avoid conflicts between tests
    public static String getRandomString(int length) {
        // Generate short random alphanumeric string of given length
        return random.ints(48, 123)
                .filter(i -> Character.isDigit(i) || Character.isLowerCase(i))
                .limit(length)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }

    private final VaultContainer<?> vaultContainer = new VaultContainer<>(getVaultImage())
            .withCommand("server", "-dev", "-dev-root-token-id=root", "-dev-plugin-dir=/vault/plugins")
            .withClasspathResourceMapping(getTestPluginFilename(), "/vault/plugins/test-plugin", READ_ONLY)
            .withVaultToken("root")
            .withEnv("VAULT_ADDR", "http://127.0.0.1:8200");

    private final JDKVaultHttpClient httpClient = new JDKVaultHttpClient(HttpClient.newHttpClient());
    private VaultClient vaultClient;
    private URL vaultBaseUrl;

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {

        var annotation = extensionContext.getRequiredTestClass().getAnnotation(VaultClientTest.class);

        vaultContainer
                .withEnv("VAULT_LOG_LEVEL", annotation.logLevel())
                .start();

        vaultClient = VaultClient.builder()
                .executor(httpClient)
                .baseUrl(vaultContainer.getHttpHostAddress())
                .clientToken("root")
                .build();

        vaultBaseUrl = new URL(vaultContainer.getHttpHostAddress());

        var secretMounts = annotation.secrets();
        for (var secretMount : secretMounts) {
            var cmd = "vault secrets enable -path=" + secretMount.path() + " " +
                    String.join(" ", secretMount.options()) + " " + secretMount.type();
            var result = vaultContainer.execInContainer("/bin/sh", "-c", cmd);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Failed to enable Vault engine '" + secretMount.type() + "' at mount path '" +
                        secretMount.path() + "': " + result.getStderr());
            }
        }

        var authMounts = annotation.auths();
        for (var authMount : authMounts) {
            var cmd = "vault auth enable -path=" + authMount.path() + " " +
                    String.join(" ", authMount.options()) + " " + authMount.type();
            var result = vaultContainer.execInContainer("/bin/sh", "-c", cmd);
            if (result.getExitCode() != 0) {
                throw new RuntimeException("Failed to enable Vault auth '" + authMount.type() + "' at mount path '" +
                        authMount.path() + "': " + result.getStderr());
            }
        }
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        vaultContainer.stop();
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        var param = parameterContext.getParameter();
        var paramType = param.getType();

        if (paramType.equals(VaultClient.class)) {
            return true;
        } else if (paramType.equals(String.class) && param.getAnnotation(Random.class) != null) {
            return true;
        } else if ((paramType.equals(URL.class) || paramType.equals(String.class))
                && param.getAnnotation(Vault.class) != null) {
            return true;
        }
        return false;
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext)
            throws ParameterResolutionException {

        var param = parameterContext.getParameter();
        var paramType = param.getType();

        if (paramType.equals(VaultClient.class)) {
            return vaultClient;
        } else if (paramType.equals(String.class) && param.getAnnotation(Random.class) != null) {
            return getRandomString(8);
        } else if (param.getAnnotation(Vault.class) != null) {
            if (paramType.equals(URL.class)) {
                return vaultBaseUrl;
            } else {
                return vaultBaseUrl.toString();
            }
        }
        return null;
    }

    public static String getTestPluginFilename() {
        return "vault-test-plugin-linux-" + getPluginArchitecture();
    }

    public static String getPluginArchitecture() {
        var osArch = System.getProperty("os.arch");
        return osArch.contains("aarch") || osArch.contains("arm") ? "arm64" : "amd64";
    }

    public static String getPluginSha256() throws Exception {
        try (var pluginStream = VaultSysPluginsTest.class.getResourceAsStream("/" + getTestPluginFilename())) {
            if (pluginStream == null) {
                throw new IOException("Test plugin not found as resource");
            }

            var digest = MessageDigest.getInstance("SHA-256");
            ;
            digest.update(pluginStream.readAllBytes());
            return HexFormat.of().formatHex(digest.digest());
        }
    }

}

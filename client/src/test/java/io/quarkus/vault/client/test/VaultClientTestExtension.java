package io.quarkus.vault.client.test;

import java.net.URL;
import java.net.http.HttpClient;

import org.junit.jupiter.api.extension.*;
import org.testcontainers.vault.VaultContainer;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;

@VaultClientTest
public class VaultClientTestExtension implements BeforeAllCallback, AfterAllCallback, ParameterResolver {

    private static final java.util.Random random = new java.util.Random();

    public static final String DEFAULT_VAULT_VERSION = "1.15.2";

    public static String getVaultVersion() {
        var version = System.getenv("VAULT_VERSION");
        return version != null ? version : DEFAULT_VAULT_VERSION;
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

    private final VaultContainer<?> vaultContainer = new VaultContainer<>("hashicorp/vault:" + getVaultVersion())
            .withEnv("VAULT_LOG_LEVEL", "debug")
            .withVaultToken("root");
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
}

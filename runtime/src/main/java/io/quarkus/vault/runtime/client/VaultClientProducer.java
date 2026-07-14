package io.quarkus.vault.runtime.client;

import java.nio.file.Path;

import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.auth.VaultAppRoleAuthOptions;
import io.quarkus.vault.client.auth.VaultGithubAuthOptions;
import io.quarkus.vault.client.auth.VaultKubernetesAuthOptions;
import io.quarkus.vault.client.auth.VaultStaticClientTokenAuthOptions;
import io.quarkus.vault.client.auth.VaultUserPassAuthOptions;
import io.quarkus.vault.client.http.VaultHttpClient;
import io.quarkus.vault.client.http.jdk.JDKVaultHttpClient;
import io.quarkus.vault.client.http.vertx.VertxVaultHttpClient;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.vertx.core.Vertx;

@Singleton
public class VaultClientProducer {

    @Produces
    @Singleton
    @Private
    public VaultClient privateVaultClient(VaultConfigHolder vaultConfigHolder,
            @ConfigProperty(name = "quarkus.tls.trust-all", defaultValue = "false") boolean globalTrustAll) {

        var config = vaultConfigHolder.getVaultRuntimeConfig();

        var httpClient = JDKClientFactory.createHttpClient(config, globalTrustAll);
        var vaultHttpClient = new JDKVaultHttpClient(httpClient);

        return createVaultClient(vaultHttpClient, config);
    }

    @Produces
    @Singleton
    public VaultClient sharedVaultClient(Vertx vertx, VaultConfigHolder vaultConfigHolder,
            @ConfigProperty(name = "quarkus.tls.trust-all", defaultValue = "false") boolean globalTrustAll) {

        var config = vaultConfigHolder.getVaultRuntimeConfig();

        var webClient = MutinyVertxClientFactory.createHttpClient(vertx, config, globalTrustAll);
        var vaultHttpClient = new VertxVaultHttpClient(webClient);

        return createVaultClient(vaultHttpClient, config);
    }

    VaultClient createVaultClient(VaultHttpClient vaultHttpClient, VaultRuntimeConfig config) {

        var vaultClientBuilder = VaultClient.builder()
                .baseUrl(config.url().orElseThrow(() -> new VaultException("no vault url provided")))
                .executor(vaultHttpClient)
                .requestTimeout(config.readTimeout())
                .logConfidentialityLevel(config.logConfidentialityLevel());

        configureAuthentication(vaultClientBuilder, config);

        if (config.enterprise().namespace().isPresent()) {
            vaultClientBuilder.namespace(config.enterprise().namespace().orElseThrow());
        }

        return vaultClientBuilder.build();
    }

    void configureAuthentication(VaultClient.Builder builder, VaultRuntimeConfig config) {

        var authConfig = config.authentication();

        if (authConfig.none()) {

            if (authConfig.isDirectClientToken() || authConfig.isAppRole() || authConfig.isUserpass()
                    || authConfig.isGithub() || authConfig.isAwsIam()
                    || authConfig.kubernetes().role().isPresent()) {
                throw new VaultException("'quarkus.vault.authentication.none' is exclusive with all other " +
                        "authentication properties; remove the other authentication settings or set 'none' to false");
            }

            // no token provider: requests are sent without a token, e.g. through a Vault Agent with auto-auth enabled

        } else if (authConfig.isDirectClientToken()) {

            if (authConfig.clientTokenWrappingToken().isPresent()) {
                builder.clientToken(VaultStaticClientTokenAuthOptions.builder()
                        .unwrappingToken(authConfig.clientTokenWrappingToken().orElseThrow())
                        .build());
            } else {
                builder.clientToken(authConfig.clientToken().orElseThrow());
            }

        } else {

            var authenticationType = config.getAuthenticationType();
            if (authenticationType == null) {
                throw new VaultException("no vault authentication configured; if this is intentional, e.g. when " +
                        "using a Vault Agent with auto-auth enabled, set 'quarkus.vault.authentication.none' to true");
            }

            switch (authenticationType) {

                case KUBERNETES:
                    var k8sConfig = authConfig.kubernetes();

                    builder.kubernetes(
                            VaultKubernetesAuthOptions.builder()
                                    .mountPath(k8sConfig.authMountPath())
                                    .role(k8sConfig.role().orElseThrow())
                                    .jwtTokenPath(Path.of(k8sConfig.jwtTokenPath()))
                                    .caching(config.renewGracePeriod())
                                    .build());
                    break;

                case APPROLE:
                    var appRoleConfig = authConfig.appRole();

                    var appRoleOptions = VaultAppRoleAuthOptions.builder()
                            .mountPath(appRoleConfig.authMountPath())
                            .roleId(appRoleConfig.roleId().orElseThrow());
                    if (appRoleConfig.secretIdWrappingToken().isPresent()) {
                        appRoleOptions.unwrappingSecretId(appRoleConfig.secretIdWrappingToken().orElseThrow());
                    } else {
                        appRoleOptions.secretId(appRoleConfig.secretId().orElseThrow());
                    }
                    appRoleOptions.caching(config.renewGracePeriod());

                    builder.appRole(appRoleOptions.build());
                    break;

                case USERPASS:
                    var userPassConfig = authConfig.userpass();

                    var userPassOptions = VaultUserPassAuthOptions.builder()
                            .mountPath(userPassConfig.authMountPath())
                            .username(userPassConfig.username().orElseThrow());
                    if (userPassConfig.passwordWrappingToken().isPresent()) {
                        userPassOptions.unwrappingPassword(userPassConfig.passwordWrappingToken().orElseThrow(),
                                config.kvSecretEngineVersion());
                    } else {
                        userPassOptions.password(userPassConfig.password().orElseThrow());
                    }
                    userPassOptions.caching(config.renewGracePeriod());

                    builder.userPass(userPassOptions.build());
                    break;

                case GITHUB:
                    var githubConfig = authConfig.github();

                    var githubOptions = VaultGithubAuthOptions.builder()
                            .mountPath(githubConfig.authMountPath());
                    if (githubConfig.tokenWrappingToken().isPresent()) {
                        githubOptions.unwrappingToken(githubConfig.tokenWrappingToken().orElseThrow(),
                                config.kvSecretEngineVersion());
                    } else {
                        githubOptions.token(githubConfig.token().orElseThrow());
                    }
                    githubOptions.caching(config.renewGracePeriod());

                    builder.github(githubOptions.build());
                    break;

                case AWS_IAM:
                    // Isolated so that the optional AWS SDK is referenced from exactly one place,
                    // which can be substituted away in native images when the SDK is absent.
                    VaultAwsIamAuthConfigurator.configure(builder, config);
                    break;

                default:
                    throw new VaultException("Unsupported authentication type: " + config.getAuthenticationType());
            }
        }
    }

}

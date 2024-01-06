package io.quarkus.vault.runtime.client;

import java.nio.file.Path;

import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;

import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.auth.VaultAppRoleAuthOptions;
import io.quarkus.vault.client.auth.VaultKubernetesAuthOptions;
import io.quarkus.vault.client.auth.VaultStaticClientTokenAuthOptions;
import io.quarkus.vault.client.auth.VaultUserPassAuthOptions;
import io.quarkus.vault.client.http.vertx.VertxVaultHttpClient;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;

@Singleton
public class VaultClientProducer {

    @Produces
    @Singleton
    @Private
    public VaultClient privateVaultClient(@Private Vertx vertx, VaultConfigHolder vaultConfigHolder,
            TlsConfig tlsConfig) {

        var config = vaultConfigHolder.getVaultRuntimeConfig();

        return createVaultClient(MutinyVertxClientFactory.createHttpClient(vertx, config, tlsConfig), config);
    }

    @Produces
    @Singleton
    public VaultClient sharedVaultClient(Vertx vertx, VaultConfigHolder vaultConfigHolder, TlsConfig tlsConfig) {

        var config = vaultConfigHolder.getVaultRuntimeConfig();

        return createVaultClient(MutinyVertxClientFactory.createHttpClient(vertx, config, tlsConfig), config);
    }

    VaultClient createVaultClient(WebClient webClient, VaultRuntimeConfig config) {

        var vaultHttpClient = new VertxVaultHttpClient(webClient);

        var vaultClientBuilder = VaultClient.builder()
                .baseUrl(config.url().orElseThrow(() -> new VaultException("no vault url provided")))
                .executor(vaultHttpClient)
                .logConfidentialityLevel(config.logConfidentialityLevel());

        configureAuthentication(vaultClientBuilder, config);

        if (config.enterprise().namespace().isPresent()) {
            vaultClientBuilder.namespace(config.enterprise().namespace().orElseThrow());
        }

        return vaultClientBuilder.build();
    }

    void configureAuthentication(VaultClient.Builder builder, VaultRuntimeConfig config) {

        var authConfig = config.authentication();

        if (authConfig.isDirectClientToken()) {

            if (authConfig.clientTokenWrappingToken().isPresent()) {
                builder.clientToken(VaultStaticClientTokenAuthOptions.builder()
                        .unwrappingToken(authConfig.clientTokenWrappingToken().orElseThrow())
                        .build());
            } else {
                builder.clientToken(authConfig.clientToken().orElseThrow());
            }

        } else {

            switch (config.getAuthenticationType()) {

                case KUBERNETES:
                    builder.kubernetes(
                            VaultKubernetesAuthOptions.builder()
                                    .role(authConfig.kubernetes().role().orElseThrow())
                                    .jwtTokenPath(Path.of(authConfig.kubernetes().jwtTokenPath()))
                                    .caching(config.renewGracePeriod())
                                    .build());
                    break;

                case APPROLE:
                    var appRoleConfig = authConfig.appRole();

                    var appRoleOptions = VaultAppRoleAuthOptions.builder()
                            .roleId(appRoleConfig.roleId().orElseThrow());
                    if (appRoleConfig.secretIdWrappingToken().isPresent()) {
                        appRoleOptions.unwrappingSecretId(appRoleConfig.secretIdWrappingToken().orElseThrow());
                    } else {
                        appRoleOptions.secretId(appRoleConfig.secretId().orElseThrow());
                    }

                    builder.appRole(appRoleOptions.build());
                    break;

                case USERPASS:
                    var userPassConfig = authConfig.userpass();

                    var userPassOptions = VaultUserPassAuthOptions.builder()
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

                default:
                    throw new VaultException("Unsupported authentication type: " + config.getAuthenticationType());
            }
        }
    }

    @Produces
    @Singleton
    @Private
    public Vertx privateVertx() {
        return Vertx.vertx();
    }

    public void destroyPrivateVertx(@Disposes @Private Vertx vertx) {
        vertx.close().await().indefinitely();
    }

}

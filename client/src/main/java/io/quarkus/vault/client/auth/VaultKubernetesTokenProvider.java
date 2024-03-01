package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Supplier;
import java.util.logging.Logger;

import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetes;
import io.quarkus.vault.client.common.VaultResponse;

public class VaultKubernetesTokenProvider implements VaultTokenProvider {
    private static final Logger log = Logger.getLogger(VaultKubernetesTokenProvider.class.getName());

    private final String mountPath;
    private final String role;
    private final Supplier<CompletionStage<String>> jwtProvider;

    public VaultKubernetesTokenProvider(String mountPath, String role, Supplier<CompletionStage<String>> jwtProvider) {
        this.mountPath = mountPath;
        this.role = role;
        this.jwtProvider = jwtProvider;
    }

    public VaultKubernetesTokenProvider(VaultKubernetesAuthOptions options) {
        this(options.mountPath, options.role, options.jwtProvider);
    }

    @Override
    public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.getExecutor();

        return jwtProvider.get().thenCompose(jwt -> {

            log.fine("authenticating with kubernetes jwt: " +
                    authRequest.getRequest().getLogConfidentialityLevel().maskWithTolerance(jwt, LOW));

            return executor.execute(VaultAuthKubernetes.FACTORY.login(mountPath, role, jwt))
                    .thenApply(VaultResponse::getResult)
                    .thenApply(res -> {
                        var auth = res.getAuth();
                        return VaultToken.from(auth.getClientToken(), auth.isRenewable(), auth.getLeaseDuration(),
                                authRequest.getInstantSource());
                    });
        });
    }

    public static Supplier<CompletionStage<String>> jwtTokenPathReader(Path path) {
        return () -> CompletableFuture.completedStage(readJwtToken(path));
    }

    private static String readJwtToken(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JWT token from " + path, e);
        }
    }
}

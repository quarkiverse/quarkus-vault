package io.quarkus.vault.client.auth;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.logging.Logger;

import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetes;
import io.smallrye.mutiny.Uni;

public class VaultKubernetesTokenProvider implements VaultTokenProvider {
    private static final Logger log = Logger.getLogger(VaultKubernetesTokenProvider.class.getName());

    private final String mountPath;
    private final String role;
    private final Supplier<Uni<String>> jwtProvider;

    public VaultKubernetesTokenProvider(String mountPath, String role, Supplier<Uni<String>> jwtProvider) {
        this.mountPath = mountPath;
        this.role = role;
        this.jwtProvider = jwtProvider;
    }

    public VaultKubernetesTokenProvider(VaultKubernetesAuthOptions options) {
        this(options.mountPath, options.role, options.jwtProvider);
    }

    @Override
    public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
        var executor = authRequest.executor();

        return jwtProvider.get().flatMap(jwt -> {

            log.fine("authenticating with kubernetes jwt: " +
                    authRequest.request().getLogConfidentialityLevel().maskWithTolerance(jwt, LOW));

            return executor.execute(VaultAuthKubernetes.FACTORY.login(mountPath, role, jwt))
                    .map(result -> VaultToken.from(result.auth.clientToken, result.auth.renewable,
                            result.auth.leaseDuration));
        });
    }

    public static Supplier<Uni<String>> jwtTokenPathReader(Path path) {
        return () -> Uni.createFrom().item(readJwtToken(path));
    }

    private static String readJwtToken(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read JWT token from " + path, e);
        }
    }
}

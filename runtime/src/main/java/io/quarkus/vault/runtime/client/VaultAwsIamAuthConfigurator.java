package io.quarkus.vault.runtime.client;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.auth.VaultAwsIamAuthOptions;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

/**
 * Configures AWS IAM authentication on a {@link VaultClient.Builder}.
 * <p>
 * This is deliberately isolated in its own class, and exposes no AWS SDK types in its signature, so
 * that it is the single point where the (optional) AWS SDK is referenced. When the AWS SDK is not on
 * the classpath, a GraalVM substitution replaces {@link #configure} during native image builds, which
 * keeps the AWS SDK out of the closed-world analysis for applications that do not use AWS IAM
 * authentication.
 *
 * @see io.quarkus.vault.runtime.graal.Target_VaultAwsIamAuthConfigurator
 */
public final class VaultAwsIamAuthConfigurator {

    private VaultAwsIamAuthConfigurator() {
    }

    public static void configure(VaultClient.Builder builder, VaultRuntimeConfig config) {

        var awsIamConfig = config.authentication().awsIam();

        var awsIamOptions = VaultAwsIamAuthOptions.builder()
                .mountPath(awsIamConfig.authMountPath())
                .role(awsIamConfig.role().orElseThrow())
                .region(awsIamConfig.region().orElseThrow(
                        () -> new VaultException("region is required for AWS IAM authentication")))
                .stsUrl(awsIamConfig.stsUrl())
                .caching(config.renewGracePeriod());
        awsIamConfig.vaultServerId().ifPresent(awsIamOptions::vaultServerId);
        if (awsIamConfig.awsAccessKey().isPresent() && awsIamConfig.awsSecretKey().isPresent()) {
            awsIamOptions.staticCredentials(awsIamConfig.awsAccessKey().orElseThrow(),
                    awsIamConfig.awsSecretKey().orElseThrow());
        }

        builder.awsIam(awsIamOptions.build());
    }
}

package io.quarkus.vault.runtime.config;

import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_AWS_IAM_AUTH_MOUNT_PATH;
import static io.quarkus.vault.runtime.config.VaultRuntimeConfig.DEFAULT_AWS_IAM_STS_URL;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultAwsIamAuthenticationConfig {

    /**
     * AWS IAM authentication role that has been created in Vault to associate Vault policies with an
     * AWS IAM principal (ARN). This property is required when selecting the AWS IAM authentication type.
     */
    Optional<String> role();

    /**
     * The AWS region used to sign the {@code sts:GetCallerIdentity} request. This property is required
     * when selecting the AWS IAM authentication type.
     */
    Optional<String> region();

    /**
     * The URL of the AWS STS endpoint used to build the {@code GetCallerIdentity} request.
     */
    @WithDefault(DEFAULT_AWS_IAM_STS_URL)
    String stsUrl();

    /**
     * The Vault server ID sent in the {@code X-Vault-AWS-IAM-Server-ID} header of the signed request,
     * used by Vault to mitigate replay attacks. Must match the {@code iam_server_id_header_value}
     * configured on the Vault AWS auth method.
     */
    Optional<String> vaultServerId();

    /**
     * The AWS access key ID used to sign the request. When both the access key and secret key are set,
     * static credentials are used; otherwise the AWS default credentials provider chain is used.
     */
    Optional<String> awsAccessKey();

    /**
     * The AWS secret access key used to sign the request. When both the access key and secret key are
     * set, static credentials are used; otherwise the AWS default credentials provider chain is used.
     */
    Optional<String> awsSecretKey();

    /**
     * Allows configuring the AWS IAM authentication mount path.
     */
    @WithDefault(DEFAULT_AWS_IAM_AUTH_MOUNT_PATH)
    String authMountPath();
}

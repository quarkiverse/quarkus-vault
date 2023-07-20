package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultAwsIamAuthenticationConfig {

    /**
     * AWS IAM authentication role that has been created in Vault to associate Vault policies, with
     * ARN. This property is required when selecting the AWS IAM authentication type.
     */
    @ConfigItem
    public Optional<String> role;

    /**
     * The AWS region to use for AWS IAM authentication.
     */
    @ConfigItem
    public Optional<String> region;

    /**
     * The URL of the AWS STS endpoint to use for AWS IAM authentication.
     */
    @ConfigItem(defaultValue = "https://sts.amazonaws.com")
    public String stsUrl;

    /**
     * The Vault server ID to use for AWS IAM authentication.
     */
    @ConfigItem
    public Optional<String> vaultServerId;

    /**
     * The AWS access key ID to use for AWS IAM authentication.
     */
    @ConfigItem
    public Optional<String> awsAccessKey;

    /**
     * The AWS secret access key to use for AWS IAM authentication.
     */
    @ConfigItem
    public Optional<String> awsSecretKey;
}

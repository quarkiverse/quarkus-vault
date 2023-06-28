package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultAwsIamAuthenticationConfig {

    /**
     * Aws iam authentication role that has been created in Vault to associate Vault policies, with
     * aws iam service accounts. This property is required when selecting
     * the aws iam authentication type.
     */
    @ConfigItem
    public String role;

    @ConfigItem
    public String region;

    @ConfigItem(defaultValue = "https://sts.amazonaws.com")
    public String stsUrl;

    @ConfigItem
    public Optional<String> vaultServerId;
}

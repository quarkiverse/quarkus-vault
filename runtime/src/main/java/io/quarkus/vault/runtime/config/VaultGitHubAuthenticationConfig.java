package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;

@ConfigGroup
public class VaultGitHubAuthenticationConfig {

    /**
     * Token for GitHub auth method. This property is required when selecting the github authentication type.
     */
    @ConfigItem
    public Optional<String> token;

}

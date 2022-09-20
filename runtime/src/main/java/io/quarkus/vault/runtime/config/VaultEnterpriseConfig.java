package io.quarkus.vault.runtime.config;

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface VaultEnterpriseConfig {

    /**
     * Vault Enterprise namespace
     * <p>
     * If set, this will add a `X-Vault-Namespace` header to all requests sent to the Vault server.
     * <p>
     * See https://www.vaultproject.io/docs/enterprise/namespaces
     *
     * @asciidoclet
     */
    Optional<String> namespace();
}

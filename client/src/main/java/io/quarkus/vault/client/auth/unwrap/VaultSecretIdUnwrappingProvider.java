package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdResult;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault AppRole secret IDs generated with the AppRole engine's
 * {@link VaultAuthAppRoleGenerateSecretIdResult Generate Secret ID}.
 */
public class VaultSecretIdUnwrappingProvider extends VaultUnwrappingValueProvider<VaultAuthAppRoleGenerateSecretIdResult> {

    public VaultSecretIdUnwrappingProvider(String wrappingToken) {
        super(wrappingToken);
    }

    @Override
    public String getType() {
        return "secret id";
    }

    @Override
    public Class<? extends VaultAuthAppRoleGenerateSecretIdResult> getUnwrapResultType() {
        return VaultAuthAppRoleGenerateSecretIdResult.class;
    }

    @Override
    public String extractClientToken(VaultAuthAppRoleGenerateSecretIdResult vaultAppRoleGenerateNewSecretID) {
        return vaultAppRoleGenerateNewSecretID.getData().getSecretId();
    }
}

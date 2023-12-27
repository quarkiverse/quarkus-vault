package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdResult;
import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdResultData;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault AppRole secret IDs generated with the AppRole engine's
 * {@link VaultAuthAppRoleGenerateSecretIdResult Generate Secret ID}.
 */
public class VaultSecretIdUnwrappingProvider extends VaultUnwrappingValueProvider<VaultAuthAppRoleGenerateSecretIdResultData> {

    public VaultSecretIdUnwrappingProvider(String wrappingToken) {
        super(wrappingToken);
    }

    @Override
    public String getType() {
        return "secret id";
    }

    @Override
    public Class<? extends VaultAuthAppRoleGenerateSecretIdResultData> getUnwrapResultType() {
        return VaultAuthAppRoleGenerateSecretIdResultData.class;
    }

    @Override
    public String extractClientToken(VaultAuthAppRoleGenerateSecretIdResultData vaultAppRoleGenerateNewSecretId) {
        return vaultAppRoleGenerateNewSecretId.getSecretId();
    }
}

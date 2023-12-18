package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.approle.VaultAuthAppRoleGenerateSecretIdResult;

public class VaultSecretIdUnwrappingProvider extends VaultUnwrappingTokenProvider<VaultAuthAppRoleGenerateSecretIdResult> {

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
        return vaultAppRoleGenerateNewSecretID.data.secretId;
    }
}

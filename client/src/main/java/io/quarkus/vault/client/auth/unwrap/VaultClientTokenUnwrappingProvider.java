package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateResult;

public class VaultClientTokenUnwrappingProvider extends VaultUnwrappingTokenProvider<VaultAuthTokenCreateResult> {

    public VaultClientTokenUnwrappingProvider(String unwrappingToken) {
        super(unwrappingToken);
    }

    @Override
    public String getType() {
        return "client token";
    }

    @Override
    public Class<VaultAuthTokenCreateResult> getUnwrapResultType() {
        return null;
    }

    @Override
    public String extractClientToken(VaultAuthTokenCreateResult result) {
        return result.auth.clientToken;
    }
}

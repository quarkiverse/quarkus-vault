package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateResult;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault client tokens generated with the Token engine's
 * {@link VaultAuthTokenCreateResult Create Token}.
 */
public class VaultClientTokenUnwrappingProvider extends VaultUnwrappingValueProvider<VaultAuthTokenCreateResult> {

    public VaultClientTokenUnwrappingProvider(String wrappingToken) {
        super(wrappingToken);
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
        return result.getAuth().getClientToken();
    }
}

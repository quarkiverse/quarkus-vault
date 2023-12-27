package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateAuthResult;
import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateResult;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault client tokens generated with the Token engine's
 * {@link VaultAuthTokenCreateResult Create Token}.
 */
public class VaultClientTokenUnwrappingProvider extends VaultUnwrappingValueProvider<VaultAuthTokenCreateAuthResult> {

    public VaultClientTokenUnwrappingProvider(String wrappingToken) {
        super(wrappingToken);
    }

    @Override
    public String getType() {
        return "client token";
    }

    @Override
    public Class<VaultAuthTokenCreateAuthResult> getUnwrapResultType() {
        return VaultAuthTokenCreateAuthResult.class;
    }

    @Override
    public String extractClientToken(VaultAuthTokenCreateAuthResult result) {
        return result.getClientToken();
    }
}

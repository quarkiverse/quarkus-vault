package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.secrets.kv.VaultSecretsKVReadResult;
import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1ReadResult;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretResult;

public class VaultPasswordUnwrappingTokenProvider extends VaultUnwrappingTokenProvider<VaultSecretsKVReadResult> {

    public static final String UNWRAPPED_PASSWORD_KEY = "password";

    private final int version;

    public VaultPasswordUnwrappingTokenProvider(String wrappingToken, int version) {
        super(wrappingToken);
        this.version = version;
    }

    @Override
    public String getType() {
        return "password";
    }

    @Override
    public Class<? extends VaultSecretsKVReadResult> getUnwrapResultType() {
        return version == 1 ? VaultSecretsKV1ReadResult.class : VaultSecretsKV2ReadSecretResult.class;
    }

    @Override
    public String extractClientToken(VaultSecretsKVReadResult result) {
        return result.getData().get(UNWRAPPED_PASSWORD_KEY).toString();
    }
}

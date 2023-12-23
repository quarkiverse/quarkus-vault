package io.quarkus.vault.client.auth.unwrap;

import io.quarkus.vault.client.api.secrets.kv.VaultSecretsKVReadResult;
import io.quarkus.vault.client.api.secrets.kv1.VaultSecretsKV1ReadResult;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretResult;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault KV secrets generated with the KV engine's
 * {@link VaultSecretsKVReadResult Read Secret}.
 */
public class VaultKeyValueUnwrappingValueProvider extends VaultUnwrappingValueProvider<VaultSecretsKVReadResult> {

    private final String valueKey;
    private final int version;

    public VaultKeyValueUnwrappingValueProvider(String wrappingToken, String valueKey, int version) {
        super(wrappingToken);
        this.valueKey = valueKey;
        this.version = version;
    }

    @Override
    public String getType() {
        return valueKey + " in kv(" + version + ")";
    }

    @Override
    public Class<? extends VaultSecretsKVReadResult> getUnwrapResultType() {
        return version == 1 ? VaultSecretsKV1ReadResult.class : VaultSecretsKV2ReadSecretResult.class;
    }

    @Override
    public String extractClientToken(VaultSecretsKVReadResult result) {
        return result.getValues().get(valueKey).toString();
    }
}

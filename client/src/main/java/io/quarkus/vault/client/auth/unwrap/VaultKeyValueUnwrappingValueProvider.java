package io.quarkus.vault.client.auth.unwrap;

import java.util.Map;

import io.quarkus.vault.client.api.secrets.kv.VaultSecretsKVReadResult;
import io.quarkus.vault.client.api.secrets.kv2.VaultSecretsKV2ReadSecretData;

/**
 * A {@link VaultUnwrappingValueProvider} for Vault KV secrets generated with the KV engine's
 * {@link VaultSecretsKVReadResult Read Secret}.
 */
public class VaultKeyValueUnwrappingValueProvider extends VaultUnwrappingValueProvider<Object> {

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
    public Class<?> getUnwrapResultType() {
        return version == 1 ? Map.class : VaultSecretsKV2ReadSecretData.class;
    }

    @Override
    public String extractClientToken(Object result) {
        if (version == 1) {
            return ((Map<?, ?>) result).get(valueKey).toString();
        } else {
            return ((VaultSecretsKV2ReadSecretData) result).getData().get(valueKey).toString();
        }
    }
}

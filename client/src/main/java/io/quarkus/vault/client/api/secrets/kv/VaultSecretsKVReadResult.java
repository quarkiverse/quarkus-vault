package io.quarkus.vault.client.api.secrets.kv;

import java.util.Map;

public interface VaultSecretsKVReadResult {

    Map<String, Object> getValues();

}

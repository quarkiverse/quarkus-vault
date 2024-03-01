package io.quarkus.vault.client.api.secrets.kv;

import java.util.Map;

import io.quarkus.vault.client.common.VaultModel;

public interface VaultSecretsKVReadResult extends VaultModel {

    Map<String, Object> getValues();

}

package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvSecretJsonV2Data implements VaultModel {

    public Map<String, Object> data;
    public VaultKvSecretV2Metadata metadata;

}

package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvSecretV2WriteBody implements VaultModel {

    private final Map<String, String> data;

    private Map<String, Integer> options;

    public VaultKvSecretV2WriteBody(final Map<String, String> data) {
        this.data = data;
    }

    public VaultKvSecretV2WriteBody(final Map<String, String> data,
            final Map<String, Integer> options) {
        this.data = data;
        this.options = options;
    }

    public static VaultKvSecretV2WriteBody of(final Map<String, String> data) {
        return new VaultKvSecretV2WriteBody(data);
    }

    public static VaultKvSecretV2WriteBody of(final Map<String, String> data,
            final Map<String, Integer> options) {
        return new VaultKvSecretV2WriteBody(data, options);
    }

    @JsonProperty
    public Map<String, Integer> options() {
        return options;
    }

    @JsonProperty
    public Map<String, String> data() {
        return data;
    }

}

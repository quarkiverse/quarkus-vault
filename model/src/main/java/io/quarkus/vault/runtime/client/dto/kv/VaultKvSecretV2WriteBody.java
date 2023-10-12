package io.quarkus.vault.runtime.client.dto.kv;

import java.util.Map;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultKvSecretV2WriteBody implements VaultModel {

    private final Map<String, String> data;
    private Map<String, Integer> options;

    private VaultKvSecretV2WriteBody(final Map<String, String> data) {
        this.data = data;
    }

    private VaultKvSecretV2WriteBody(final Map<String, String> data,
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

    public Map<String, Integer> options() {
        return options;
    }

    public Map<String, String> data() {
        return data;
    }
}

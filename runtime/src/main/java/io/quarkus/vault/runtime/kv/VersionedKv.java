package io.quarkus.vault.runtime.kv;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.quarkus.vault.client.api.common.VaultRequestFactory;
import io.smallrye.mutiny.Uni;

public abstract class VersionedKv<T extends VaultRequestFactory> {

    public abstract Uni<Map<String, Object>> readSecretJson(String path);

    public abstract Uni<Void> writeSecret(String path, Map<String, String> secret);

    public abstract Uni<Void> deleteSecret(String path);

    public abstract Uni<List<String>> listSecrets(String path);

    public Uni<Map<String, String>> readSecret(String path) {
        return readSecretJson(path).map(this::convert);
    }

    private Map<String, String> convert(Map<String, Object> map) {
        return map.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (String) entry.getValue()));
    }

    protected Map<String, Object> asSecretMap(Map<String, String> secret) {
        return secret.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> (Object) entry.getValue()));
    }
}

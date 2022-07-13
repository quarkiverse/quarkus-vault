package io.quarkus.vault.runtime.client;

import java.util.Map;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;

public interface VaultClient {

    String X_VAULT_TOKEN = "X-Vault-Token";
    String X_VAULT_NAMESPACE = "X-Vault-Namespace";
    String API_VERSION = "v1";

    <T> Uni<T> put(String path, String token, Object body, int expectedCode);

    <T> Uni<T> list(String path, String token, Class<T> resultClass);

    <T> Uni<T> delete(String path, String token, int expectedCode);

    <T> Uni<T> post(String path, String token, Object body, Class<T> resultClass, int expectedCode);

    <T> Uni<T> post(String path, String token, Object body, Class<T> resultClass);

    <T> Uni<T> post(String path, String token, Map<String, String> headers, Object body, Class<T> resultClass);

    <T> Uni<T> post(String path, String token, Object body, int expectedCode);

    <T> Uni<T> put(String path, String token, Object body, Class<T> resultClass);

    <T> Uni<T> put(String path, Object body, Class<T> resultClass);

    <T> Uni<T> get(String path, String token, Class<T> resultClass);

    <T> Uni<T> get(String path, Map<String, String> queryParams, Class<T> resultClass);

    Uni<Buffer> get(String path, String token);

    Uni<Integer> head(String path);

    Uni<Integer> head(String path, Map<String, String> queryParams);

    Uni<Void> close();
}

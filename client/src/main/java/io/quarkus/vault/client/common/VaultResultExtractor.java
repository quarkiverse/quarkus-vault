package io.quarkus.vault.client.common;

public interface VaultResultExtractor<T> {

    T extract(VaultResponse<T> response);

}

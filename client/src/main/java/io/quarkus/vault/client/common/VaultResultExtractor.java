package io.quarkus.vault.client.common;

import java.util.Optional;

public interface VaultResultExtractor<T> {

    Optional<T> extract(VaultResponse<T> response);

}

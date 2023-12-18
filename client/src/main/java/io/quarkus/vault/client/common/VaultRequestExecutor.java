package io.quarkus.vault.client.common;

import io.smallrye.mutiny.Uni;

public interface VaultRequestExecutor {

    <T> Uni<T> execute(VaultRequest<T> request);

}

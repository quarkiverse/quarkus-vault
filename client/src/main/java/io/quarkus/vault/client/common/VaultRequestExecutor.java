package io.quarkus.vault.client.common;

import java.util.concurrent.CompletionStage;

public interface VaultRequestExecutor {

    <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request);

}

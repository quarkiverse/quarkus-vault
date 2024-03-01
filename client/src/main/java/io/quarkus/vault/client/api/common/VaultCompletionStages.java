package io.quarkus.vault.client.api.common;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import io.quarkus.vault.client.VaultClientException;

public class VaultCompletionStages {

    public static <T> Function<Throwable, CompletionStage<T>> recoverNotFound(Supplier<T> valueSupplier) {
        return x -> {
            if (x instanceof CompletionException || x instanceof ExecutionException) {
                x = x.getCause();
            }
            if (x instanceof VaultClientException vaultClientException && vaultClientException.getStatus() == 404) {
                return CompletableFuture.completedFuture(valueSupplier.get());
            }
            return CompletableFuture.failedFuture(x);
        };
    }
}

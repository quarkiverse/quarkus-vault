package io.quarkus.vault.client.util;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

public class OptionalCompletionStages {

    public static <T, U extends T> Function<Optional<T>, CompletionStage<Optional<U>>> flatMapPresent(
            Function<T, CompletionStage<U>> mapper) {
        return ot -> ot.map(t -> mapper.apply(t).thenApply(Optional::ofNullable)).orElse(empty());
    }

    public static <T> Function<CompletionStage<Optional<T>>, CompletionStage<Optional<T>>> flatMapEmpty(
            Supplier<CompletionStage<T>> mapper) {
        return cs -> cs.thenCompose(ot -> ot.map(OptionalCompletionStages::createFrom)
                .orElseGet(() -> mapper.get().thenApply(Optional::ofNullable)));
    }

    public static <T> Function<Optional<T>, CompletionStage<T>> flatMapEmptyGet(Supplier<CompletionStage<T>> mapper) {
        return ot -> ot.map(CompletableFuture::completedStage).orElseGet(mapper);
    }

    public static <T, U extends T> Function<CompletionStage<Optional<T>>, CompletionStage<Optional<U>>> mapPresent(
            Function<T, U> mapper) {
        return cs -> cs.thenApply(ot -> ot.map(mapper));
    }

    public static <T> Function<CompletionStage<Optional<T>>, CompletionStage<Optional<T>>> mapEmpty(Supplier<T> mapper) {
        return cs -> cs.thenApply(ot -> ot.or(() -> Optional.ofNullable(mapper.get())));
    }

    public static <T> CompletionStage<Optional<T>> empty() {
        return CompletableFuture.completedStage(Optional.empty());
    }

    public static <T> CompletionStage<Optional<T>> createFrom(T value) {
        return CompletableFuture.completedStage(Optional.of(value));
    }

    public static <T> CompletionStage<Optional<T>> createFromNullable(T value) {
        return CompletableFuture.completedStage(Optional.ofNullable(value));
    }

}

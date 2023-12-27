package io.quarkus.vault.client.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import io.smallrye.mutiny.Uni;

public class OptionalUnis {

    public static <T, U extends T> Function<Uni<Optional<T>>, Uni<Optional<U>>> flatMapPresent(Function<T, Uni<U>> mapper) {
        return uni -> uni.flatMap(ot -> ot.map(t -> mapper.apply(t).map(Optional::ofNullable)).orElse(empty()));
    }

    public static <T> Function<Uni<Optional<T>>, Uni<Optional<T>>> flatMapEmpty(Supplier<Uni<T>> mapper) {
        return uni -> uni.flatMap(ot -> ot.map(OptionalUnis::createFrom)
                .orElseGet(() -> mapper.get().map(Optional::ofNullable)));
    }

    public static <T> Function<Uni<Optional<T>>, Uni<T>> flatMapEmptyGet(Supplier<Uni<T>> mapper) {
        return uni -> uni.flatMap(ot -> ot.map(t -> Uni.createFrom().item(t)).orElseGet(mapper));
    }

    public static <T, U extends T> Function<Uni<Optional<T>>, Uni<Optional<U>>> mapPresent(Function<T, U> mapper) {
        return uni -> uni.map(ot -> ot.map(mapper));
    }

    public static <T> Function<Uni<Optional<T>>, Uni<Optional<T>>> mapEmpty(Supplier<T> mapper) {
        return uni -> uni.map(ot -> ot.or(() -> Optional.ofNullable(mapper.get())));
    }

    public static <T> Uni<Optional<T>> empty() {
        return Uni.createFrom().item(Optional.empty());
    }

    public static <T> Uni<Optional<T>> createFrom(T value) {
        return Uni.createFrom().item(Optional.of(value));
    }

    public static <T> Uni<Optional<T>> createFromNullable(T value) {
        return Uni.createFrom().item(Optional.ofNullable(value));
    }

}

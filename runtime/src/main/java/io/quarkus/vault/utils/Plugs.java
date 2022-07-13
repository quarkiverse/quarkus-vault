package io.quarkus.vault.utils;

import java.util.Optional;

import io.quarkus.vault.runtime.client.VaultClientException;
import io.smallrye.mutiny.Uni;

public class Plugs {

    public static <T> Uni<Optional<T>> notFoundToEmpty(Uni<Optional<T>> stream) {
        return stream.onFailure(VaultClientException.class).recoverWithUni((e) -> {
            if (((VaultClientException) e).getStatus() == 404) {
                return Uni.createFrom().item(Optional.empty());
            } else {
                return Uni.createFrom().failure(e);
            }
        });
    }

}

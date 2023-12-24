package io.quarkus.vault.client.api.common;

import java.util.function.Predicate;

import io.quarkus.vault.client.VaultClientException;

public class VaultNotFoundPredicate implements Predicate<Throwable> {

    public static final VaultNotFoundPredicate INSTANCE = new VaultNotFoundPredicate();

    @Override
    public boolean test(Throwable e) {
        if (e instanceof VaultClientException vaultClientException) {
            return vaultClientException.getStatus() == 404;
        }
        return false;
    }
}

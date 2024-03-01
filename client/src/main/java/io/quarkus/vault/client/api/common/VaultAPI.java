package io.quarkus.vault.client.api.common;

import java.time.Duration;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;

import io.quarkus.vault.client.common.VaultLeasedResultExtractor;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;

public class VaultAPI<F extends VaultRequestFactory> {

    protected final VaultRequestExecutor executor;
    protected final F factory;

    public VaultAPI(VaultRequestExecutor executor, F factory) {
        this.executor = executor;
        this.factory = factory;
    }

    public VaultRequestExecutor getExecutor() {
        return executor;
    }

    public <T> CompletionStage<VaultWrapInfo> wrapping(Duration wrapTTL, Function<F, VaultRequest<T>> builder) {
        var request = builder.apply(factory);
        var wrapRequest = request.builder().wrapTTL(wrapTTL)
                .build(VaultLeasedResultExtractor.of(VaultWrappedResult.class));
        return executor.execute(wrapRequest)
                .thenApply(r -> r.getResult().getWrapInfo());
    }

}

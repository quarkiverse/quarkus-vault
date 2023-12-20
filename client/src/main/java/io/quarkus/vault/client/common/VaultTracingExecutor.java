package io.quarkus.vault.client.common;

import java.util.logging.Logger;

import io.smallrye.mutiny.Uni;

public class VaultTracingExecutor implements VaultRequestExecutor {

    private static final Logger log = Logger.getLogger(VaultTracingExecutor.class.getName());

    private final VaultRequestExecutor delegate;

    public VaultTracingExecutor(VaultRequestExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {
        log.info("Executing request: " + request.getOperation());
        return delegate.execute(request)
                .onItem().invoke((result) -> log.info("Request successful: " + request.getOperation()))
                .onFailure().invoke((error) -> log.info("Request failed: " + error));
    }
}

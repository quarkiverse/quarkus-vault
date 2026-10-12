package io.quarkus.vault.runtime.config;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.runtime.LaunchMode;

/**
 * Keeps, for the Dev UI only, the property names loaded by the Vault config source per KV path, with the fetch time and
 * the last error. Only property names are kept, never values, and nothing is recorded outside of dev mode.
 */
public final class VaultConfigSourceStatus {

    public record PathStatus(String engine, String path, String prefix, List<String> propertyNames, String error) {
    }

    public record Snapshot(String fetchedAt, List<PathStatus> paths) {
    }

    private static final AtomicReference<Snapshot> LAST = new AtomicReference<>();

    private VaultConfigSourceStatus() {
    }

    static void record(List<PathStatus> paths) {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT) {
            return;
        }
        LAST.set(new Snapshot(Instant.now().toString(), List.copyOf(paths)));
    }

    public static Optional<Snapshot> last() {
        return Optional.ofNullable(LAST.get());
    }
}

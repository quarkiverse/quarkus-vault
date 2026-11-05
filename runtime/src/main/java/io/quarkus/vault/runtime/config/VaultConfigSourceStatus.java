package io.quarkus.vault.runtime.config;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import io.quarkus.vault.client.VaultClientException;

/**
 * Keeps, for the Dev UI only, the property names served by the Vault config source per KV path, with the time of the
 * last successful fetch and the last error. Only property names and error categories are kept, never values or raw
 * error messages. The Vault config source records it in dev mode only.
 */
public final class VaultConfigSourceStatus {

    public record PathStatus(String engine, String path, String prefix, List<String> propertyNames, String error) {
    }

    /**
     * @param fetchedAt time of the fetch the paths come from
     * @param paths property names or error per KV path
     * @param refreshError error category of the last failed refresh, while the last known values are still served
     * @param refreshFailedAt time of the last failed refresh
     */
    public record Snapshot(String fetchedAt, List<PathStatus> paths, String refreshError, String refreshFailedAt) {
    }

    private static final AtomicReference<Snapshot> LAST = new AtomicReference<>();

    private VaultConfigSourceStatus() {
    }

    static void recordFetch(List<PathStatus> paths) {
        LAST.set(new Snapshot(Instant.now().toString(), List.copyOf(paths), null, null));
    }

    /**
     * Keeps the paths of the previous snapshot, since the config source keeps serving the last known values after a
     * failed refresh, and annotates them with the error.
     */
    static void recordFailedRefresh(Throwable error) {
        String failedAt = Instant.now().toString();
        LAST.updateAndGet(last -> last == null
                ? null
                : new Snapshot(last.fetchedAt(), last.paths(), errorCategory(error), failedAt));
    }

    static void clear() {
        LAST.set(null);
    }

    public static Optional<Snapshot> last() {
        return Optional.ofNullable(LAST.get());
    }

    /**
     * Describes an error without its message, which can contain internal URLs or paths.
     */
    public static String errorCategory(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof VaultClientException clientException && clientException.getStatus() != null) {
                return switch (clientException.getStatus()) {
                    case 403 -> "permission denied (403)";
                    case 404 -> "not found (404)";
                    default -> "HTTP " + clientException.getStatus();
                };
            }
            if (current instanceof IOException) {
                return "unreachable";
            }
            if (current instanceof TimeoutException) {
                return "timeout";
            }
        }
        return error.getClass().getSimpleName();
    }
}

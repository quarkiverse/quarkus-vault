package io.quarkus.vault.runtime.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.UncheckedIOException;
import java.net.ConnectException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.VaultException;

class VaultConfigSourceStatusTest {

    private static final VaultConfigSourceStatus.PathStatus PATH = new VaultConfigSourceStatus.PathStatus("<default>",
            "a", null, List.of("db-url"), null);

    @AfterEach
    void clear() {
        VaultConfigSourceStatus.clear();
    }

    @Test
    void failedRefreshKeepsTheLastSuccessfulPaths() {
        VaultConfigSourceStatus.recordFetch(List.of(PATH));
        String fetchedAt = VaultConfigSourceStatus.last().orElseThrow().fetchedAt();

        VaultConfigSourceStatus.recordFailedRefresh(new VaultException(new ConnectException("Connection refused")));

        VaultConfigSourceStatus.Snapshot snapshot = VaultConfigSourceStatus.last().orElseThrow();
        assertEquals(List.of(PATH), snapshot.paths());
        assertEquals(fetchedAt, snapshot.fetchedAt());
        assertEquals("unreachable", snapshot.refreshError());
        assertTrue(snapshot.refreshFailedAt() != null);
    }

    @Test
    void successfulFetchClearsThePreviousRefreshError() {
        VaultConfigSourceStatus.recordFetch(List.of(PATH));
        VaultConfigSourceStatus.recordFailedRefresh(new VaultException(new ConnectException("Connection refused")));

        VaultConfigSourceStatus.recordFetch(List.of(PATH));

        assertNull(VaultConfigSourceStatus.last().orElseThrow().refreshError());
    }

    @Test
    void failedRefreshWithoutPreviousFetchRecordsNothing() {
        VaultConfigSourceStatus.recordFailedRefresh(new VaultException(new ConnectException("Connection refused")));

        assertTrue(VaultConfigSourceStatus.last().isEmpty());
    }

    @Test
    void errorCategoryHidesTheMessage() {
        assertEquals("not found (404)", VaultConfigSourceStatus.errorCategory(clientError(404)));
        assertEquals("permission denied (403)", VaultConfigSourceStatus.errorCategory(clientError(403)));
        assertEquals("HTTP 500", VaultConfigSourceStatus.errorCategory(clientError(500)));
        assertEquals("unreachable", VaultConfigSourceStatus.errorCategory(
                new VaultClientException("READ", "http://internal-host:8200/v1/secret/a", null, (String) null,
                        new UncheckedIOException(new ConnectException("internal-host")))));
        assertEquals("timeout", VaultConfigSourceStatus.errorCategory(new CompletionException(new TimeoutException())));
        assertEquals("IllegalStateException",
                VaultConfigSourceStatus.errorCategory(new IllegalStateException("http://internal-host")));
    }

    private static VaultClientException clientError(int status) {
        return new VaultClientException("READ", "http://internal-host:8200/v1/secret/a", status, (String) null, null);
    }
}

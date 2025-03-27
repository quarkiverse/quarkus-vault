package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.Duration;
import java.time.InstantSource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.auth.VaultToken;
import io.quarkus.vault.client.auth.VaultTokenProvider;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;

public class VaultClientRetryTest {

    @Test
    public void testPermissionDeniedInvalidatesCacheAndRetriesOnce() throws Exception {

        var executor = spy(new VaultRequestExecutor() {
            @Override
            public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
                return CompletableFuture
                        .failedStage(new VaultClientException("Test", "/test", 403, List.of("permission denied"), null));
            }
        });
        var tokenProvider = spy(new VaultTokenProvider() {
            VaultToken token = VaultToken.from("baaaaad", true, Duration.ofMinutes(1), null, InstantSource.system()).cached();

            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return CompletableFuture.completedStage(token);
            }

            @Override
            public void invalidateCache() {
                // simulate token refresh
                token = VaultToken.from(token.getClientToken(), token.isRenewable(), token.getLeaseDuration(),
                        token.getAllowedUsesRemaining(), token.getInstantSource());
            }
        });

        var client = VaultClient.builder()
                .baseUrl("https://example.com:8200")
                .executor(executor)
                .tokenProvider(tokenProvider)
                .build();

        assertThatThrownBy(() -> client.secrets().kv1().list().toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasMessageContaining("permission denied");

        verify(tokenProvider, times(2))
                .apply(any());
        verify(executor, times(2))
                .execute(any());
    }

    @Test
    public void testBuilder() throws Exception {
        var executor = new VaultRequestExecutor() {
            @Override
            public <T> CompletionStage<VaultResponse<T>> execute(VaultRequest<T> request) {
                return CompletableFuture.completedStage(null);
            }
        };
        var tokenProvider = new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return CompletableFuture.completedStage(null);
            }
        };

        var client = VaultClient.builder()
                .baseUrl("https://example.com:8200")
                .apiVersion("v2")
                .executor(executor)
                .tokenProvider(tokenProvider)
                .requestTimeout(Duration.ofHours(2))
                .namespace("test-ns")
                .logConfidentialityLevel(LogConfidentialityLevel.MEDIUM)
                .build();

        assertThat(client.getApiVersion())
                .isEqualTo("v2");
        assertThat(client.getBaseUrl())
                .isEqualTo(new URL("https://example.com:8200"));
        assertThat(client.getExecutor())
                .isEqualTo(executor);
        assertThat(client.getRequestTimeout())
                .isEqualTo(Duration.ofHours(2));
        assertThat(client.getTokenProvider())
                .isEqualTo(tokenProvider);
        assertThat(client.getNamespace())
                .isEqualTo("test-ns");
        assertThat(client.getLogConfidentialityLevel())
                .isEqualTo(LogConfidentialityLevel.MEDIUM);
    }

}

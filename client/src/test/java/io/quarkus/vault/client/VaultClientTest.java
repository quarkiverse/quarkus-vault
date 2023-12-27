package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import java.net.URL;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.auth.VaultToken;
import io.quarkus.vault.client.auth.VaultTokenProvider;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultRequestExecutor;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.client.logging.LogConfidentialityLevel;
import io.smallrye.mutiny.Uni;

public class VaultClientTest {

    @Test
    public void testPermissionDeniedInvalidatesCacheAndRetriesOnce() {

        var executor = spy(new VaultRequestExecutor() {
            @Override
            public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {
                return Uni.createFrom().failure(new VaultClientException("Test", "/test", 403, List.of("permission denied")));
            }
        });
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from("test", true, Duration.ofMinutes(1), authRequest.getInstantSource())
                        .cached();
                return Uni.createFrom().item(token);
            }
        });

        var client = VaultClient.builder()
                .baseUrl("https://example.com:8200")
                .executor(executor)
                .tokenProvider(tokenProvider)
                .build();

        assertThatThrownBy(() -> client.secrets().kv1().list().await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("permission denied");

        verify(tokenProvider, times(1))
                .apply(any());
        verify(executor, times(2))
                .execute(any());
    }

    @Test
    public void testBuilder() throws Exception {
        var executor = new VaultRequestExecutor() {
            @Override
            public <T> Uni<VaultResponse<T>> execute(VaultRequest<T> request) {
                return Uni.createFrom().nullItem();
            }
        };
        var tokenProvider = new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                return Uni.createFrom().nullItem();
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

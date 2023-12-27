package io.quarkus.vault.client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateTokenParams;
import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.auth.VaultCachingTokenProvider;
import io.quarkus.vault.client.auth.VaultToken;
import io.quarkus.vault.client.auth.VaultTokenProvider;
import io.quarkus.vault.client.test.TickableInstanceSource;
import io.quarkus.vault.client.test.VaultClientTest;
import io.smallrye.mutiny.Uni;

@VaultClientTest
public class VaultCachingTokenProviderTest {

    private static final TickableInstanceSource tickableInstanceSource = new TickableInstanceSource(Instant.now());

    @Test
    public void testExpiredTokensAreRequestedAgain(VaultClient client) {

        var token = client.auth().token().create(null)
                .map(a -> VaultToken.from(a.getClientToken(), true, Duration.ofMinutes(1), tickableInstanceSource))
                .await().indefinitely();
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                return Uni.createFrom().item(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstanceSource)
                .executor(executor)
                .tokenProvider(cachingTokenProvider)
                .build();

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(10))
                .execute(any());
        verify(tokenProvider, times(1))
                .apply(any());
        verify(cachingTokenProvider, times(1))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());

        tickableInstanceSource.tick(Duration.ofSeconds(90));

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(20))
                .execute(any());
        verify(tokenProvider, times(2))
                .apply(any());
        verify(cachingTokenProvider, times(2))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());
    }

    @Test
    public void testExpiringTokensRenew(VaultClient client) {

        var token = client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                .setTtl(Duration.ofMinutes(1))
                .setRenewable(true))
                .map(a -> VaultToken.from(a.getClientToken(), a.isRenewable(), a.getLeaseDuration(), tickableInstanceSource))
                .await().indefinitely();
        //noinspection Convert2Lambda
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                return Uni.createFrom().item(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstanceSource)
                .executor(executor)
                .tokenProvider(cachingTokenProvider)
                .build();

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(10))
                .execute(any());
        verify(tokenProvider, times(1))
                .apply(any());
        verify(cachingTokenProvider, times(1))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());

        tickableInstanceSource.tick(Duration.ofSeconds(45));

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(21))
                .execute(any());
        verify(tokenProvider, times(1))
                .apply(any());
        verify(cachingTokenProvider, times(1))
                .request(any());
        verify(cachingTokenProvider, times(1))
                .extend(any(), any());
    }

    @Test
    public void testExpiredNonRenewableTokensAreRequestedAgain(VaultClient client) {

        var token = client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                .setTtl(Duration.ofMinutes(5))
                .setRenewable(false))
                .map(a -> VaultToken.from(a.getClientToken(), a.isRenewable(), Duration.ofMinutes(1), tickableInstanceSource))
                .await().indefinitely();
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                return Uni.createFrom().item(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstanceSource)
                .executor(executor)
                .tokenProvider(cachingTokenProvider)
                .build();

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(10))
                .execute(any());
        verify(tokenProvider, times(1))
                .apply(any());
        verify(cachingTokenProvider, times(1))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());

        tickableInstanceSource.tick(Duration.ofSeconds(90));

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(20))
                .execute(any());
        verify(tokenProvider, times(2))
                .apply(any());
        verify(cachingTokenProvider, times(2))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());
    }

    @Test
    public void testExpiringTokensFailingToRenewAreRequestedAgain(VaultClient client) {

        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public Uni<VaultToken> apply(VaultAuthRequest authRequest) {
                return client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                        .setTtl(Duration.ofSeconds(30))
                        .setRenewable(false))
                        .map(a -> VaultToken.from(a.getClientToken(), true, Duration.ofMinutes(1), tickableInstanceSource));
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstanceSource)
                .executor(executor)
                .tokenProvider(cachingTokenProvider)
                .build();

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(10))
                .execute(any());
        verify(tokenProvider, times(1))
                .apply(any());
        verify(cachingTokenProvider, times(1))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());

        tickableInstanceSource.tick(Duration.ofSeconds(45));

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .await().indefinitely();
        }

        verify(executor, times(21))
                .execute(any());
        verify(tokenProvider, times(2))
                .apply(any());
        verify(cachingTokenProvider, times(2))
                .request(any());
        verify(cachingTokenProvider, times(1))
                .extend(any(), any());
    }
}

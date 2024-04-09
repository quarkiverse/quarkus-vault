package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateTokenParams;
import io.quarkus.vault.client.api.common.VaultAuthResult;
import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.auth.VaultCachingTokenProvider;
import io.quarkus.vault.client.auth.VaultToken;
import io.quarkus.vault.client.auth.VaultTokenProvider;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.test.TickableInstanceSource;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultCachingTokenProviderTest {

    private static final TickableInstanceSource tickableInstanceSource = new TickableInstanceSource(Instant.now());

    @Test
    public void testCachedTokensAreNotChanged(VaultClient client) throws Exception {

        var clientToken = client.auth().token().create(null)
                .thenApply(VaultAuthResult::getClientToken)
                .toCompletableFuture().get();
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, true, Duration.ofMinutes(1), tickableInstanceSource);
                return CompletableFuture.completedStage(token);
            }
        });
        var cachingTokenProvider = new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30));

        var authRequest = new VaultAuthRequest(client.getExecutor(), VaultRequest.get("test").build(), tickableInstanceSource);

        var originalToken = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken1 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken2 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken3 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();

        assertThat(originalToken.isFromCache())
                .isFalse();
        assertThat(cachedToken1.isFromCache())
                .isTrue();
        assertThat(cachedToken2.isFromCache())
                .isTrue();
        assertThat(cachedToken3.isFromCache())
                .isTrue();

        assertThat(cachedToken1.getClientToken())
                .isEqualTo(originalToken.getClientToken());
        assertThat(cachedToken2.getClientToken())
                .isEqualTo(originalToken.getClientToken());
        assertThat(cachedToken3.getClientToken())
                .isEqualTo(originalToken.getClientToken());

        assertThat(cachedToken1.getCreated())
                .isEqualTo(originalToken.getCreated());
        assertThat(cachedToken2.getCreated())
                .isEqualTo(originalToken.getCreated());
        assertThat(cachedToken3.getCreated())
                .isEqualTo(originalToken.getCreated());

        assertThat(cachedToken1.getLeaseDuration())
                .isEqualTo(originalToken.getLeaseDuration());
        assertThat(cachedToken2.getLeaseDuration())
                .isEqualTo(originalToken.getLeaseDuration());
        assertThat(cachedToken3.getLeaseDuration())
                .isEqualTo(originalToken.getLeaseDuration());

        assertThat(cachedToken1.isRenewable())
                .isEqualTo(originalToken.isRenewable());
        assertThat(cachedToken2.isRenewable())
                .isEqualTo(originalToken.isRenewable());
        assertThat(cachedToken3.isRenewable())
                .isEqualTo(originalToken.isRenewable());
    }

    @Test
    public void testExpiredTokensAreRequestedAgain(VaultClient client) throws Exception {

        var clientToken = client.auth().token().create(null)
                .thenApply(VaultAuthResult::getClientToken)
                .toCompletableFuture().get();
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, true, Duration.ofMinutes(1), tickableInstanceSource);
                return CompletableFuture.completedStage(token);
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
                    .toCompletableFuture().get();
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
                    .toCompletableFuture().get();
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
    public void testExpiringTokensRenew(VaultClient client) throws Exception {

        var token = client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                .setTtl(Duration.ofMinutes(1))
                .setRenewable(true))
                .thenApply(
                        a -> VaultToken.from(a.getClientToken(), a.isRenewable(), a.getLeaseDuration(), tickableInstanceSource))
                .toCompletableFuture().get();
        //noinspection Convert2Lambda
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return CompletableFuture.completedStage(token);
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
                    .toCompletableFuture().get();
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
                    .toCompletableFuture().get();
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
    public void testExpiredNonRenewableTokensAreRequestedAgain(VaultClient client) throws Exception {

        var clientToken = client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                .setTtl(Duration.ofMinutes(5))
                .setRenewable(false))
                .thenApply(VaultAuthResult::getClientToken)
                .toCompletableFuture().get();
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, false, Duration.ofMinutes(1), tickableInstanceSource);
                return CompletableFuture.completedStage(token);
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
                    .toCompletableFuture().get();
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
                    .toCompletableFuture().get();
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
    public void testExpiringTokensFailingToRenewAreRequestedAgain(VaultClient client) throws Exception {

        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                        .setTtl(Duration.ofSeconds(30))
                        .setRenewable(false))
                        .thenApply(
                                a -> VaultToken.from(a.getClientToken(), true, Duration.ofMinutes(1), tickableInstanceSource));
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
                    .toCompletableFuture().get();
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
                    .toCompletableFuture().get();
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

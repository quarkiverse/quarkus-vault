package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.as;
import static org.assertj.core.api.InstanceOfAssertFactories.BOOLEAN;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
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
import io.quarkus.vault.client.test.TickableInstantSource;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultCachingTokenProviderTest {

    private static final TickableInstantSource tickableInstantSource = new TickableInstantSource(Instant.now());

    @Test
    public void testCachedTokensAreNotChanged(VaultClient client) throws Exception {

        var clientToken = client.auth().token().create(null)
                .thenApply(VaultAuthResult::getClientToken)
                .toCompletableFuture().get();
        //noinspection Convert2Lambda
        var tokenProvider = spy((VaultTokenProvider) new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, true, Duration.ofMinutes(1), null, tickableInstantSource);
                return CompletableFuture.completedStage(token);
            }
        });
        var cachingTokenProvider = new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30));

        var authRequest = new VaultAuthRequest(client.getExecutor(), VaultRequest.get("test").build(), tickableInstantSource);

        var originalToken = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken1 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken2 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();
        var cachedToken3 = cachingTokenProvider.apply(authRequest)
                .toCompletableFuture().get();

        assertThat(List.of(cachedToken1, cachedToken2, cachedToken3))
                .allSatisfy(s -> {
                    assertThat(s)
                            .extracting(VaultToken::isFromCache, as(BOOLEAN))
                            .isTrue();
                    assertThat(s)
                            .extracting(VaultToken::getClientToken)
                            .isEqualTo(originalToken.getClientToken());
                    assertThat(s)
                            .extracting(VaultToken::getCreated)
                            .isEqualTo(originalToken.getCreated());
                    assertThat(s)
                            .extracting(VaultToken::getLeaseDuration)
                            .isEqualTo(originalToken.getLeaseDuration());
                    assertThat(s)
                            .extracting(VaultToken::isRenewable)
                            .isEqualTo(originalToken.isRenewable());
                });
    }

    @Test
    public void testExpiredTokensAreRequestedAgain(VaultClient client) throws Exception {

        var clientToken = client.auth().token().create(null)
                .thenApply(VaultAuthResult::getClientToken)
                .toCompletableFuture().get();
        //noinspection Convert2Lambda
        var tokenProvider = spy((VaultTokenProvider) new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, true, Duration.ofMinutes(1), null, tickableInstantSource);
                return CompletableFuture.completedStage(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstantSource)
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

        tickableInstantSource.tick(Duration.ofSeconds(90));

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
                        a -> VaultToken.from(a.getClientToken(), a.isRenewable(), a.getLeaseDuration(), a.getNumUses(),
                                tickableInstantSource))
                .toCompletableFuture().get();
        //noinspection Convert2Lambda
        var tokenProvider = spy((VaultTokenProvider) new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return CompletableFuture.completedStage(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstantSource)
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

        tickableInstantSource.tick(Duration.ofSeconds(45));

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
        //noinspection Convert2Lambda
        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                var token = VaultToken.from(clientToken, false, Duration.ofMinutes(1), null, tickableInstantSource);
                return CompletableFuture.completedStage(token);
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstantSource)
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

        tickableInstantSource.tick(Duration.ofSeconds(90));

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
                                a -> VaultToken.from(a.getClientToken(), true, Duration.ofMinutes(1), a.getNumUses(),
                                        tickableInstantSource));
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstantSource)
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

        tickableInstantSource.tick(Duration.ofSeconds(45));

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

    @Test
    public void testLimitedUseTokensAreRequestedAgain(VaultClient client) throws Exception {

        var tokenProvider = spy(new VaultTokenProvider() {
            @Override
            public CompletionStage<VaultToken> apply(VaultAuthRequest authRequest) {
                return client.auth().token().create(new VaultAuthTokenCreateTokenParams()
                        .setTtl(Duration.ofMinutes(5))
                        .setRenewable(false)
                        .setNumUses(2))
                        .thenApply(
                                a -> VaultToken.from(a.getClientToken(), a.isRenewable(), a.getLeaseDuration(), a.getNumUses(),
                                        tickableInstantSource));
            }
        });
        var cachingTokenProvider = spy(new VaultCachingTokenProvider(tokenProvider, Duration.ofSeconds(30)));
        var executor = spy(client.getExecutor());

        var testClient = client.configure()
                .instantSource(tickableInstantSource)
                .executor(executor)
                .tokenProvider(cachingTokenProvider)
                .build();

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .toCompletableFuture().get();
        }

        verify(executor, times(10))
                .execute(any());
        verify(tokenProvider, times(5))
                .apply(any());
        verify(cachingTokenProvider, times(5))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());

        tickableInstantSource.tick(Duration.ofSeconds(90));

        for (int i = 0; i < 10; i++) {
            testClient.secrets().kv2().listSecrets("/")
                    .toCompletableFuture().get();
        }

        verify(executor, times(20))
                .execute(any());
        verify(tokenProvider, times(10))
                .apply(any());
        verify(cachingTokenProvider, times(10))
                .request(any());
        verify(cachingTokenProvider, times(0))
                .extend(any(), any());
    }
}

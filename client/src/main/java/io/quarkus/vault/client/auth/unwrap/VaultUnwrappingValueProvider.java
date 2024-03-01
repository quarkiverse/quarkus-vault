package io.quarkus.vault.client.auth.unwrap;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.time.Duration;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.sys.wrapping.VaultSysWrapping;
import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.json.JsonMapping;

public abstract class VaultUnwrappingValueProvider<UnwrapResult> implements VaultValueProvider {
    private static final Logger log = Logger.getLogger(VaultUnwrappingValueProvider.class.getName());
    private static final Cache<String, CompletionStage<String>> unwrappingCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1)).build();
    private final String wrappingToken;

    protected VaultUnwrappingValueProvider(String wrappingToken) {
        this.wrappingToken = wrappingToken;
    }

    public abstract String getType();

    public abstract Class<? extends UnwrapResult> getUnwrapResultType();

    public abstract String extractClientToken(UnwrapResult result);

    @Override
    public CompletionStage<String> apply(VaultAuthRequest unwrapRequest) {
        return unwrappingCache.get(wrappingToken, (token) -> {
            var executor = unwrapRequest.getExecutor();
            return executor.execute(VaultSysWrapping.FACTORY.unwrap(token))
                    .thenApply(response -> {
                        var result = response.getResult();
                        var value = result.getAuth() != null ? result.getAuth() : result.getData();
                        var unwrappedValue = JsonMapping.mapper.convertValue(value, getUnwrapResultType());
                        var unwrappedClientToken = extractClientToken(unwrappedValue);

                        String displayValue = unwrapRequest.getRequest().getLogConfidentialityLevel()
                                .maskWithTolerance(unwrappedClientToken, LOW);
                        log.fine("unwrapped " + getType() + ": " + displayValue);

                        return unwrappedClientToken;
                    })
                    .exceptionally(e -> {
                        if (e instanceof CompletionException || e instanceof ExecutionException) {
                            e = e.getCause();
                        }

                        if (e instanceof VaultClientException ve && ve.getStatus() == 400) {
                            String message = "wrapping token is not valid or does not exist; " +
                                    "this means that the token has already expired " +
                                    "(if so you can increase the ttl on the wrapping token) or " +
                                    "has been consumed by somebody else " +
                                    "(potentially indicating that the wrapping token has been stolen)";
                            throw ve.withError(message);
                        } else if (e instanceof RuntimeException re) {
                            throw re;
                        } else {
                            throw new RuntimeException(e);
                        }
                    });
        });
    }
}

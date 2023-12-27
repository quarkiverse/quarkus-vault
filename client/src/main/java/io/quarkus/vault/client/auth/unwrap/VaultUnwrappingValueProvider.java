package io.quarkus.vault.client.auth.unwrap;

import static io.quarkus.vault.client.logging.LogConfidentialityLevel.LOW;

import java.time.Duration;
import java.util.logging.Logger;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.api.sys.wrapping.VaultSysWrapping;
import io.quarkus.vault.client.auth.VaultAuthRequest;
import io.quarkus.vault.client.json.JsonMapping;
import io.smallrye.mutiny.Uni;

public abstract class VaultUnwrappingValueProvider<UnwrapResult> implements VaultValueProvider {
    private static final Logger log = Logger.getLogger(VaultUnwrappingValueProvider.class.getName());
    private static final Cache<String, Uni<String>> unwrappingCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofHours(1)).build();
    private final String wrappingToken;

    protected VaultUnwrappingValueProvider(String wrappingToken) {
        this.wrappingToken = wrappingToken;
    }

    public abstract String getType();

    public abstract Class<? extends UnwrapResult> getUnwrapResultType();

    public abstract String extractClientToken(UnwrapResult result);

    @Override
    public Uni<String> apply(VaultAuthRequest unwrapRequest) {
        return unwrappingCache.get(wrappingToken, (token) -> {
            var executor = unwrapRequest.getExecutor();
            return executor.execute(VaultSysWrapping.FACTORY.unwrap(token))
                    .map(response -> {
                        var result = response.getResult();
                        var value = result.getAuth() != null ? result.getAuth() : result.getData();
                        var unwrappedValue = JsonMapping.mapper.convertValue(value, getUnwrapResultType());
                        var unwrappedClientToken = extractClientToken(unwrappedValue);

                        String displayValue = unwrapRequest.getRequest().getLogConfidentialityLevel()
                                .maskWithTolerance(unwrappedClientToken, LOW);
                        log.fine("unwrapped " + getType() + ": " + displayValue);

                        return unwrappedClientToken;
                    })
                    .onFailure(VaultClientException.class).transform(e -> {
                        if (((VaultClientException) e).getStatus() == 400) {
                            String message = "wrapping token is not valid or does not exist; " +
                                    "this means that the token has already expired " +
                                    "(if so you can increase the ttl on the wrapping token) or " +
                                    "has been consumed by somebody else " +
                                    "(potentially indicating that the wrapping token has been stolen)";
                            return new VaultException(message, e);
                        } else {
                            return e;
                        }
                    })
                    .memoize().indefinitely();
        });
    }
}

package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateAuthResult;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysWrappingTest {

    @Test
    public void testApiWrapping(VaultClient client) {

        var wrapInfo = client.auth().token().wrapping(Duration.ofSeconds(60), f -> f.create(null))
                .await().indefinitely();

        assertThat(wrapInfo)
                .isNotNull();
        assertThat(wrapInfo.token)
                .isNotEmpty();
        assertThat(wrapInfo.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(wrapInfo.ttl)
                .isEqualTo(60);
        assertThat(wrapInfo.creationPath)
                .isEqualTo("auth/token/create");

        var unwrapped = client.sys().wrapping().unwrapAs(wrapInfo.token, VaultAuthTokenCreateAuthResult.class)
                .await().indefinitely();

        assertThat(unwrapped)
                .isNotNull();
        assertThat(unwrapped.clientToken)
                .isNotEmpty();
        assertThat(unwrapped.accessor)
                .isNotEmpty();
        assertThat(unwrapped.policies)
                .containsExactly("root");
        assertThat(unwrapped.tokenPolicies)
                .containsExactly("root");
        assertThat(unwrapped.metadata)
                .isNull();
        assertThat(unwrapped.leaseDuration)
                .isEqualTo(0);
        assertThat(unwrapped.renewable)
                .isFalse();
        assertThat(unwrapped.entityId)
                .isEmpty();
        assertThat(unwrapped.tokenType)
                .isEqualTo("service");
        assertThat(unwrapped.orphan)
                .isFalse();
        assertThat(unwrapped.numUses)
                .isEqualTo(0);
    }

    @Test
    public void testWrap(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofSeconds(60))
                .await().indefinitely();

        assertThat(wrapped.token)
                .isNotEmpty();
        assertThat(wrapped.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(wrapped.ttl)
                .isEqualTo(60);
        assertThat(wrapped.creationPath)
                .isEqualTo("sys/wrapping/wrap");
    }

    @Test
    public void testLookup(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofSeconds(60))
                .await().indefinitely();

        assertThat(wrapped.token)
                .isNotEmpty();

        var lookup = wrappingApi.lookup(wrapped.token)
                .await().indefinitely();

        assertThat(lookup)
                .isNotNull();
        assertThat(lookup.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(lookup.creationTtl)
                .isEqualTo(60);
        assertThat(lookup.creationPath)
                .isEqualTo("sys/wrapping/wrap");
    }

    @Test
    public void testUnwrap(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofSeconds(60))
                .await().indefinitely();

        var unwrapped = wrappingApi.unwrap(wrapped.token)
                .await().indefinitely();

        assertThat(unwrapped)
                .isNotNull();
        assertThat(unwrapped.data)
                .containsKey("data")
                .hasValueSatisfying(new Condition<>() {
                    @Override
                    public boolean matches(Object value) {

                        if (value instanceof Map<?, ?> map) {
                            return map.containsKey("foo")
                                    && map.get("foo").equals("bar");
                        } else {
                            return false;
                        }
                    }
                });
    }

    @Test
    public void testRewrap(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofSeconds(60))
                .await().indefinitely();

        var rewrapped = wrappingApi.rewrap(wrapped.token)
                .await().indefinitely();

        assertThat(rewrapped)
                .isNotNull();
        assertThat(rewrapped.token)
                .isNotEmpty();
        assertThat(rewrapped.creationTime)
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(rewrapped.ttl)
                .isEqualTo(60);
        assertThat(rewrapped.creationPath)
                .isEqualTo("sys/wrapping/wrap");
    }

}

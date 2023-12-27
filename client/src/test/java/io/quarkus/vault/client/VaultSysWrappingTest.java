package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;

import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateAuthResult;
import io.quarkus.vault.client.api.common.VaultTokenType;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultSysWrappingTest {

    @Test
    public void testApiWrapping(VaultClient client) {

        var wrapInfo = client.auth().token().wrapping(Duration.ofMinutes(1), f -> f.create(null))
                .await().indefinitely();

        assertThat(wrapInfo)
                .isNotNull();
        assertThat(wrapInfo.getToken())
                .isNotEmpty();
        assertThat(wrapInfo.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(wrapInfo.getTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(wrapInfo.getCreationPath())
                .isEqualTo("auth/token/create");

        var unwrapped = client.sys().wrapping().unwrapAs(wrapInfo.getToken(), VaultAuthTokenCreateAuthResult.class)
                .await().indefinitely();

        assertThat(unwrapped)
                .isNotNull();
        assertThat(unwrapped.getClientToken())
                .isNotEmpty();
        assertThat(unwrapped.getAccessor())
                .isNotEmpty();
        assertThat(unwrapped.getPolicies())
                .containsExactly("root");
        assertThat(unwrapped.getTokenPolicies())
                .containsExactly("root");
        assertThat(unwrapped.getMetadata())
                .isNull();
        assertThat(unwrapped.getLeaseDuration())
                .isEqualTo(Duration.ZERO);
        assertThat(unwrapped.isRenewable())
                .isFalse();
        assertThat(unwrapped.getEntityId())
                .isEmpty();
        assertThat(unwrapped.getTokenType())
                .isEqualTo(VaultTokenType.SERVICE);
        assertThat(unwrapped.isOrphan())
                .isFalse();
        assertThat(unwrapped.getNumUses())
                .isEqualTo(0);
    }

    @Test
    public void testWrap(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofMinutes(1))
                .await().indefinitely();

        assertThat(wrapped.getToken())
                .isNotEmpty();
        assertThat(wrapped.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(wrapped.getTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(wrapped.getCreationPath())
                .isEqualTo("sys/wrapping/wrap");
    }

    @Test
    public void testLookup(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofMinutes(1))
                .await().indefinitely();

        assertThat(wrapped.getToken())
                .isNotEmpty();

        var lookup = wrappingApi.lookup(wrapped.getToken())
                .await().indefinitely();

        assertThat(lookup)
                .isNotNull();
        assertThat(lookup.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(lookup.getCreationTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(lookup.getCreationPath())
                .isEqualTo("sys/wrapping/wrap");
    }

    @Test
    public void testUnwrap(VaultClient client) {
        var wrappingApi = client.sys().wrapping();

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofMinutes(1))
                .await().indefinitely();

        var unwrapped = wrappingApi.unwrap(wrapped.getToken())
                .await().indefinitely();

        assertThat(unwrapped)
                .isNotNull();
        assertThat(unwrapped.getData())
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

        var wrapped = wrappingApi.wrap(Map.of("foo", "bar"), Duration.ofMinutes(1))
                .await().indefinitely();

        var rewrapped = wrappingApi.rewrap(wrapped.getToken())
                .await().indefinitely();

        assertThat(rewrapped)
                .isNotNull();
        assertThat(rewrapped.getToken())
                .isNotEmpty();
        assertThat(rewrapped.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(rewrapped.getTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(rewrapped.getCreationPath())
                .isEqualTo("sys/wrapping/wrap");
    }

}

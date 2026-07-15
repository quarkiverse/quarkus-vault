package io.quarkus.it.vault.renew;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Pinpoint scenario for https://github.com/quarkiverse/quarkus-vault/issues/328, against the
 * in-process {@link VaultStubServer}.
 * <p>
 * The stub Vault issues login tokens with a TTL of {@link VaultStubServer#TOKEN_TTL_SECONDS} (60s)
 * and {@code quarkus.vault.renew-grace-period} is set to 40s. A Vault access performed ~35s before
 * the token expires is inside the configured grace period, so the extension is expected to extend
 * (renew-self) the login token at that point.
 */
public abstract class AbstractRenewGracePeriodTest {

    @Test
    public void tokenShouldBeRenewedWithinConfiguredGracePeriod() throws Exception {

        // first access: triggers the login
        when().get("/renew/kv").then().statusCode(200).body(is("hello"));
        assertEquals(1, VaultStubServer.LOGINS.size(), "first access should have logged in");

        // wait until we are inside the configured 40s renew grace period,
        // but still outside the client's hard-coded 30s default (~35s before expiry)
        Thread.sleep(25_000);

        // second access: the token expires in ~35s (< renew-grace-period=40s), so it must be extended
        when().get("/renew/kv").then().statusCode(200).body(is("hello"));

        assertEquals(1, VaultStubServer.LOGINS.size(),
                "the cached token is still valid, no new login expected");
        assertEquals(1, VaultStubServer.RENEWALS.size(),
                "the login token expires in ~35s, which is within the configured"
                        + " quarkus.vault.renew-grace-period=" + VaultStubServer.RENEW_GRACE_PERIOD
                        + ", so the second access should have extended the token (renew-self). If no renewal"
                        + " happened, the configured grace period was ignored and the hard-coded 30s default"
                        + " (VaultCachingTokenProvider.DEFAULT_RENEW_GRACE_PERIOD) was used instead:"
                        + " https://github.com/quarkiverse/quarkus-vault/issues/328");
    }
}

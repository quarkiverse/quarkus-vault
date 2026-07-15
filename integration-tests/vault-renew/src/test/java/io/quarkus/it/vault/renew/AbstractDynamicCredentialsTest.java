package io.quarkus.it.vault.renew;

import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.restassured.response.Response;

/**
 * End-to-end scenario for https://github.com/quarkiverse/quarkus-vault/issues/328, with a real
 * Vault and a real PostgreSQL, and the datasource configured with Vault dynamic credentials
 * (see {@link VaultPostgresTestResource}).
 * <p>
 * Timeline (login token TTL 90s, quarkus.vault.renew-grace-period=60s, credentials lease TTL 10m):
 * <ul>
 * <li>t0: first DB access — Vault login (token expires at t0+90s), dynamic DB credentials
 * generated (the lease is a child of the login token), physical connection opened and pooled.</li>
 * <li>t0+35s: a Vault access (KV read) while the token expires in ~55s, i.e. inside the configured
 * 60s renew grace period: the extension is expected to extend the login token here.</li>
 * <li>t0+105s: DB access again. If the token was extended it is valid until ~t0+125s and the
 * credentials lease is untouched. If it was NOT extended (the bug: the hard-coded 30s grace period
 * was used instead of the configured 60s), the token expired at t0+90s and Vault revoked the
 * credentials lease with it, running the revocation statements against postgres (REVOKE ALL
 * PRIVILEGES / NOLOGIN) — and the DB access fails, just like the reporter's "The server principal
 * ... is not able to access the database under the current security context".</li>
 * </ul>
 */
public abstract class AbstractDynamicCredentialsTest {

    @Test
    public void dbCredentialsShouldSurviveAsLongAsTheTokenIsUsedWithinTheConfiguredGracePeriod()
            throws Exception {

        // t0: login + dynamic credentials + first physical connection
        when().get("/renew/db").then().statusCode(200).body(is("ok"));

        // t0+35s: token expires in ~55s, inside the configured 60s grace period -> must be extended
        Thread.sleep(35_000);
        when().get("/renew/kv").then().statusCode(200).body(is("hello"));

        // t0+105s
        Thread.sleep(70_000);
        Response response = get("/renew/db");
        assertEquals(200, response.statusCode(),
                "DB access failed 105s after start: the login token was not extended at t0+35s although it was"
                        + " expiring within the configured quarkus.vault.renew-grace-period="
                        + VaultPostgresTestResource.RENEW_GRACE_PERIOD + " (the hard-coded 30s default was used"
                        + " instead), so it expired at t0+90s and Vault revoked the dynamic DB credentials lease"
                        + " along with it, revoking the DB role's privileges"
                        + " (https://github.com/quarkiverse/quarkus-vault/issues/328)."
                        + " Response: " + response.body().asString());
        assertEquals("ok", response.body().asString());
    }
}

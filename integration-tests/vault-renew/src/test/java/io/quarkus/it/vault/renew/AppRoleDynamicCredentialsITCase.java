package io.quarkus.it.vault.renew;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * FAILS on current quarkus-vault: with approle authentication the configured
 * {@code quarkus.vault.renew-grace-period} is ignored, the login token expires, and Vault revokes
 * the dynamic DB credentials lease along with it — the DB access at t0+105s fails with
 * "permission denied for table demo" (the real-Vault/real-DB materialization of issue #328).
 */
@QuarkusIntegrationTest
@WithTestResource(value = VaultPostgresTestResource.class, initArgs = @ResourceArg(name = "auth", value = "approle"))
public class AppRoleDynamicCredentialsITCase extends AbstractDynamicCredentialsTest {
}

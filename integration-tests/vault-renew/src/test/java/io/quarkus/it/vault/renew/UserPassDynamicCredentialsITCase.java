package io.quarkus.it.vault.renew;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * PASSES: identical scenario to {@link AppRoleDynamicCredentialsITCase}, but with userpass
 * authentication, for which VaultClientProducer propagates
 * {@code quarkus.vault.renew-grace-period} to the client. The login token is extended at t0+35s,
 * so it never expires while in use, the DB credentials lease stays alive, and the DB access at
 * t0+105s succeeds.
 */
@QuarkusIntegrationTest
@WithTestResource(value = VaultPostgresTestResource.class, initArgs = @ResourceArg(name = "auth", value = "userpass"))
public class UserPassDynamicCredentialsITCase extends AbstractDynamicCredentialsTest {
}

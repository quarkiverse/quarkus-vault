package io.quarkus.it.vault.renew;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * FAILS on current quarkus-vault: with approle authentication, the configured
 * {@code quarkus.vault.renew-grace-period} is ignored (VaultClientProducer never calls
 * {@code .caching(config.renewGracePeriod())} on the approle options builder), so the login token
 * is only renewed within the hard-coded 30s default grace period.
 */
@QuarkusIntegrationTest
@WithTestResource(value = VaultStubServer.class, initArgs = @ResourceArg(name = "auth", value = "approle"))
public class AppRoleRenewGracePeriodITCase extends AbstractRenewGracePeriodTest {
}

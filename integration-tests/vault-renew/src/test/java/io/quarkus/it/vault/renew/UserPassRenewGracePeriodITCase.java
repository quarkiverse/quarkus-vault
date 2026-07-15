package io.quarkus.it.vault.renew;

import io.quarkus.test.common.ResourceArg;
import io.quarkus.test.common.WithTestResource;
import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * PASSES: with userpass authentication, VaultClientProducer propagates
 * {@code quarkus.vault.renew-grace-period} to the client ({@code .caching(config.renewGracePeriod())}),
 * so the login token is renewed within the configured grace period. Same scenario as the (failing)
 * approle variant — the only difference is the authentication method.
 */
@QuarkusIntegrationTest
@WithTestResource(value = VaultStubServer.class, initArgs = @ResourceArg(name = "auth", value = "userpass"))
public class UserPassRenewGracePeriodITCase extends AbstractRenewGracePeriodTest {
}

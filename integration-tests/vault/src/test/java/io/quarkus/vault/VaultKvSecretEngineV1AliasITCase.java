package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultKvSecretEngineV1AliasITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-secret-engine-mount-path-v1.properties", "application.properties"));

    @ConfigProperty(name = "secret")
    String secret;

    @ConfigProperty(name = "myfoo.secret")
    String myfoo_secret;

    @Test
    void configSource() {
        Assertions.assertEquals(SECRET_VALUE, secret);
        Assertions.assertEquals(SECRET_VALUE, myfoo_secret);
    }
}

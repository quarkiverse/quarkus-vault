package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@QuarkusTestResource(WiremockVault.class)
public class VaultGithubTCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-github.properties", "application.properties"));

    @ConfigProperty(name = "quarkus.vault.authentication.github.token")
    String token;

    @Test
    public void testToken() {
        assertEquals("123", token);
    }

}

package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.test.VaultTestLifecycleManager;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultAwsIamITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-aws-iam.properties", "application.properties"));

    @ConfigProperty(name = "quarkus.vault.authentication.aws-iam.role")
    String role;

    @ConfigProperty(name = "quarkus.vault.authentication.aws-iam.aws-access-key")
    String key;

    @Test
    public void testRole() {
        assertEquals("myawsiamrole", role);
    }

    @Test
    public void testAwsAccessKey() {
        assertNotNull(key);
    }

}
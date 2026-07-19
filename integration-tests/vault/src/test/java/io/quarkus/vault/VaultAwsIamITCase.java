package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.APP_SECRET_PATH;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_KEY;
import static io.quarkus.vault.test.VaultTestExtension.SECRET_VALUE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Map;

import jakarta.inject.Inject;

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

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void testRoleConfig() {
        assertEquals("myawsiamrole", role);
    }

    @Test
    public void testAwsAccessKeyConfig() {
        assertNotNull(key);
    }

    @Test
    public void testSuccessAuth() {
        // reading a secret forces an AWS IAM login against Vault (signed sts:GetCallerIdentity replayed
        // against LocalStack), so a successful read proves authentication works end-to-end.
        Map<String, String> secrets = kvSecretEngine.readSecret(APP_SECRET_PATH);
        assertEquals("{" + SECRET_KEY + "=" + SECRET_VALUE + "}", secrets.toString());
    }

}

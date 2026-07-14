package io.quarkus.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(WiremockVaultAgent.class)
public class VaultAgentAutoAuthITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-agent.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void secretReadWithoutToken() {
        assertEquals("{hello=world}", kvSecretEngine.readSecret("foo").toString());
    }
}

package io.quarkus.vault;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
public class VaultAuthenticationNoneConflictITCase {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault-agent-conflict.properties", "application.properties"));

    @Inject
    VaultKVSecretEngine kvSecretEngine;

    @Test
    public void conflictingAuthenticationRejected() {
        assertThatThrownBy(() -> kvSecretEngine.readSecret("foo"))
                .hasMessageContaining("'quarkus.vault.authentication.none' is exclusive");
    }
}

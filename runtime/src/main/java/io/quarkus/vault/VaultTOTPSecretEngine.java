package io.quarkus.vault;

import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;

/**
 * This service provides access to the TOTP secret engine.
 *
 * @implNote Wrapper for reactive engine. Request timeouts are accounted for in Vault client.
 * @see <a href="https://www.vaultproject.io/api/secret/totp/index.html">TOTP Secrets Engine </a>
 */
@ApplicationScoped
public class VaultTOTPSecretEngine {

    private final VaultTOTPSecretReactiveEngine engine;

    @Inject
    public VaultTOTPSecretEngine(VaultTOTPSecretReactiveEngine engine) {
        this.engine = engine;
    }

    /**
     * Creates or updates a key definition.
     *
     * @param name of the key.
     * @param createKeyParameters required to create or update a key.
     * @return Barcode and/or URL of the created OTP key.
     */
    public Optional<KeyDefinition> createKey(String name, CreateKeyParameters createKeyParameters) {
        return engine.createKey(name, createKeyParameters).await().indefinitely();
    }

    /**
     * Queries the key definition.
     *
     * @param name of the key.
     * @return The key configuration.
     */
    public KeyConfiguration readKey(String name) {
        return engine.readKey(name).await().indefinitely();
    }

    /**
     * Returns a list of available keys. Only the key names are returned, not any values.
     *
     * @return List of available keys.
     */
    public List<String> listKeys() {
        return engine.listKeys().await().indefinitely();
    }

    /**
     * Deletes the key definition.
     *
     * @param name of the key.
     */
    public void deleteKey(String name) {
        engine.deleteKey(name).await().indefinitely();
    }

    /**
     * Generates a new time-based one-time use password based on the named key.
     *
     * @param name of the key.
     * @return The Code.
     */
    public String generateCode(String name) {
        return engine.generateCode(name).await().indefinitely();
    }

    /**
     * Validates a time-based one-time use password generated from the named key.
     *
     * @param name of the key.
     * @param code to validate.
     * @return True if valid, false otherwise.
     */
    public boolean validateCode(String name, String code) {
        return engine.validateCode(name, code).await().indefinitely();
    }
}

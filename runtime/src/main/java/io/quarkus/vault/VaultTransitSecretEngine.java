package io.quarkus.vault;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.KeyConfigRequestDetail;
import io.quarkus.vault.transit.KeyCreationRequestDetail;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SignVerifyOptions;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultDecryptionBatchException;
import io.quarkus.vault.transit.VaultEncryptionBatchException;
import io.quarkus.vault.transit.VaultRewrappingBatchException;
import io.quarkus.vault.transit.VaultSigningBatchException;
import io.quarkus.vault.transit.VaultTransitExportKeyType;
import io.quarkus.vault.transit.VaultTransitKeyDetail;
import io.quarkus.vault.transit.VaultTransitKeyExportDetail;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

/**
 * A service that interacts with Hashicorp's Vault Transit secret engine to encrypt, decrypt and sign arbitrary data.
 *
 * @implNote Wrapper for reactive engine. Request timeouts are accounted for in Vault client.
 * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#transit-secrets-engine">Transit Secrets Engine</a>
 */
@ApplicationScoped
public class VaultTransitSecretEngine {

    private final VaultTransitSecretReactiveEngine engine;

    @Inject
    public VaultTransitSecretEngine(VaultTransitSecretReactiveEngine engine) {
        this.engine = engine;
    }

    /**
     * Encrypt a regular string with a Vault key configured in the transit secret engine.
     * Equivalent to:
     * {@code encrypt(keyName, ClearData.from(clearData), null);}
     * <p>
     * This method is usually used in conjunction with {@link #decrypt(String, String)}
     *
     * @param keyName the key to encrypt the data with
     * @param clearData the string to encrypt
     * @return cipher text
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     */
    public String encrypt(String keyName, String clearData) {
        return engine.encrypt(keyName, clearData).await().indefinitely();
    }

    /**
     * Encrypt a regular string with a Vault key configured in the transit secret engine.
     * If the key does not exist, and the policy specifies a create capability the key will be lazily created
     * (i.e. upsert). The key can be further customized by specifying transit encryption-key configuration
     * properties.
     *
     * @param keyName the key to encrypt the data with
     * @param clearData the data to encrypt
     * @param transitContext optional transit context used for key derivation
     * @return cipher text
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     */
    public String encrypt(String keyName, ClearData clearData, TransitContext transitContext) {
        return engine.encrypt(keyName, clearData, transitContext).await().indefinitely();
    }

    /**
     * Encrypt a list of elements. This will return a list of cipher texts.
     * Each element shall specify the data to encrypt, an optional key version
     * and an optional transit context, used for key derivation if applicable.
     * If any error occurs, the service will throw a {@link VaultEncryptionBatchException}
     *
     * @param keyName the key to encrypt the data with
     * @param requests the list of elements to encrypt
     * @return a map of each request and its corresponding cipher text
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#encrypt-data">encrypt data</a>
     */
    public Map<EncryptionRequest, String> encrypt(String keyName, List<EncryptionRequest> requests) {
        return engine.encrypt(keyName, requests).await().indefinitely();
    }

    /**
     * Decrypt the encrypted data with the specified key, and return unencrypted data.
     *
     * @param keyName the key that was used to encrypt the original data
     * @param ciphertext the encrypted data
     * @return the unencrypted data
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     */
    public ClearData decrypt(String keyName, String ciphertext) {
        return engine.decrypt(keyName, ciphertext).await().indefinitely();
    }

    /**
     * Decrypt the encrypted data with the specified key and a transit context used for key derivation.
     *
     * @param keyName the key that was used to encrypt the original data
     * @param ciphertext data to decrypt
     * @param transitContext optional transit context used for key derivation
     * @return the unencrypted data
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#derived">create key derived attribute</a>
     */
    public ClearData decrypt(String keyName, String ciphertext, TransitContext transitContext) {
        return engine.decrypt(keyName, ciphertext, transitContext).await().indefinitely();
    }

    /**
     * Decrypt a list of encrypted data items. Each item shall specify the encrypted data plus an optional transit
     * context used for key derivation (if applicable).
     * If any error occurs, the service will throw a {@link VaultDecryptionBatchException}
     *
     * @param keyName the key that was used to encrypt the original data
     * @param requests the list of encrypted data items
     * @return a map of each request with its corresponding decrypted data item
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     */
    public Map<DecryptionRequest, ClearData> decrypt(String keyName, List<DecryptionRequest> requests) {
        return engine.decrypt(keyName, requests).await().indefinitely();
    }

    /**
     * Reencrypt into a new cipher text a cipher text that was obtained from encryption using an old key version
     * with the last key version
     *
     * @param keyName the encryption key that was used for the previous encryption
     * @param ciphertext the old cipher text that needs rewrapping
     * @return the reencrypted cipher text with last key version as a new cipher text
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     */
    public String rewrap(String keyName, String ciphertext) {
        return engine.rewrap(keyName, ciphertext).await().indefinitely();
    }

    /**
     * Reencrypt into a new cipher text a cipher text that was obtained from encryption using an old key version
     * with the last key version and an optional transit context used for key derivation
     *
     * @param keyName the encryption key that was used for the previous encryption
     * @param ciphertext the old cipher text that needs rewrapping
     * @param transitContext optional transit context used for key derivation
     * @return the reencrypted cipher text with last key version as a new cipher text
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     */
    public String rewrap(String keyName, String ciphertext, TransitContext transitContext) {
        return engine.rewrap(keyName, ciphertext, transitContext).await().indefinitely();
    }

    /**
     * Reencrypt a list of encrypted data items with the last version of the specified key.
     * Each item shall specify a cipher text to reencrypt, an optional key version, and an optional transit context
     * used for key derivation, if applicable.
     * If any error occurs, the service will throw a {@link VaultRewrappingBatchException}
     *
     * @param keyName the encryption key that was used for the previous encryptions
     * @param requests the list of items to reencrypt
     * @return a map of each request with its corresponding reencrypted data item
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#rewrap-data">rewrap data</a>
     * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#working-set-management">working set
     *      management</a>
     */
    public Map<RewrappingRequest, String> rewrap(String keyName, List<RewrappingRequest> requests) {
        return engine.rewrap(keyName, requests).await().indefinitely();
    }

    /**
     * Sign an input string with the specified key.
     *
     * @param keyName the signing key to use
     * @param input String to sign
     * @return the signature
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    public String sign(String keyName, String input) {
        return engine.sign(keyName, input).await().indefinitely();
    }

    /**
     * Sign the input with the specified key and an optional transit context used for key derivation, if applicable.
     *
     * @param keyName the signing key to use
     * @param input data to sign
     * @param transitContext optional transit context used for key derivation
     * @return the signature
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    public String sign(String keyName, SigningInput input, TransitContext transitContext) {
        return engine.sign(keyName, input, transitContext).await().indefinitely();
    }

    /**
     * Sign the input with the specified key and an optional explicit sign/verify options and an optional transit
     * context used for key derivation, if applicable.
     *
     * @param keyName the signing key to use
     * @param input data to sign
     * @param options optional explicit sign/verify options
     * @param transitContext optional transit context used for key derivation
     * @return the signature
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    public String sign(String keyName, SigningInput input, SignVerifyOptions options, TransitContext transitContext) {
        return engine.sign(keyName, input, options, transitContext).await().indefinitely();
    }

    /**
     * Sign a list of inputs items. Each item shall specify the input to sign, an optional key version, and
     * an optional transit context used for key derivation, if applicable.
     * If any error occurs, the service will throw a {@link VaultSigningBatchException}
     *
     * @param keyName the signing key to use
     * @param requests the list of inputs to sign
     * @return a map of each request with its corresponding signature item
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    public Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests) {
        return engine.sign(keyName, requests).await().indefinitely();
    }

    /**
     * Sign a list of inputs items and an optional explicit sign/verify options. Each item shall specify the input to
     * sign, an optional key version, and an optional transit context used for key derivation, if applicable.
     * If any error occurs, the service will throw a {@link VaultSigningBatchException}
     *
     * @param keyName the signing key to use
     * @param requests the list of inputs to sign
     * @param options optional explicit sign/verify options
     * @return a map of each request with its corresponding signature item
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    public Map<SigningRequest, String> sign(String keyName, List<SigningRequest> requests, SignVerifyOptions options) {
        return engine.sign(keyName, requests, options).await().indefinitely();
    }

    /**
     * Checks that the signature was obtained from signing the input with the specified key.
     * The service will throw a {@link VaultException} if this is not the case.
     *
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    public void verifySignature(String keyName, String signature, String input) {
        engine.verifySignature(keyName, signature, input).await().indefinitely();
    }

    /**
     * Checks that the signature was obtained from signing the input with the specified key.
     * The service will throw a {@link VaultException} if this is not the case.
     *
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     * @param transitContext optional transit context used for key derivation
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    public void verifySignature(String keyName, String signature, SigningInput input, TransitContext transitContext) {
        engine.verifySignature(keyName, signature, input, transitContext).await().indefinitely();
    }

    /**
     * Checks that the signature was obtained from signing the input with the specified key an an optional explicit
     * sign/verify options.
     * The service will throw a {@link VaultException} if this is not the case.
     *
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     * @param options optional explicit sign/verify options
     * @param transitContext optional transit context used for key derivation
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    public void verifySignature(String keyName, String signature, SigningInput input, SignVerifyOptions options,
            TransitContext transitContext) {
        engine.verifySignature(keyName, signature, input, options, transitContext).await().indefinitely();
    }

    /**
     * Checks a list of verification requests. Each request shall specify an input and the signature we want to match
     * against, and an optional transit context used for key derivation, if applicable. If the signature does not
     * match, or if any other error occurs, the service will throw a {@link VaultVerificationBatchException}
     *
     * @param keyName the key that was used to sign the input
     * @param requests a list of items specifying an input and a signature to match against
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    public void verifySignature(String keyName, List<VerificationRequest> requests) {
        engine.verifySignature(keyName, requests).await().indefinitely();
    }

    /**
     * Checks a list of verification requests. Each request shall specify an input and the signature we want to match
     * against, and an optional explicit sign/verify options and an optionals transit context used for key derivation,
     * if applicable. If the signature does not match, or if any other error occurs, the service will throw a
     * {@link VaultVerificationBatchException}
     *
     * @param keyName the key that was used to sign the input
     * @param requests a list of items specifying an input and a signature to match against
     * @param options optional explicit sign/verify options
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    public void verifySignature(String keyName, List<VerificationRequest> requests, SignVerifyOptions options) {
        engine.verifySignature(keyName, requests, options).await().indefinitely();
    }

    // --- admin operations

    /**
     * Create a new Transit key.
     *
     * @param keyName key name
     * @param detail key creation detail or null
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#create-key">create key</a>
     */
    public void createKey(String keyName, KeyCreationRequestDetail detail) {
        engine.createKey(keyName, detail).await().indefinitely();
    }

    /**
     * Update the configuration of a Transit key. The key must exist.
     *
     * @param keyName key name
     * @param detail key configuration detail
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#update-key-configuration">update key configuration</a>
     */
    public void updateKeyConfiguration(String keyName, KeyConfigRequestDetail detail) {
        engine.updateKeyConfiguration(keyName, detail).await().indefinitely();
    }

    /**
     * Delete a Transit key. Key must have been configured with deletion allowed. The key must exist.
     *
     * @param keyName key name
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#delete-key">delete key</a>
     */
    public void deleteKey(String keyName) {
        engine.deleteKey(keyName).await().indefinitely();
    }

    /**
     * Export a Transit Key. Key must have made exportable through creation or configuration update.
     * The key must exist.
     *
     * @param keyName name of the key
     * @param keyType key type
     * @param keyVersion null, "latest" or a valid version number as a String. If null all versions will be returned
     * @return All specified key versions
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#export-key">export key</a>
     */
    public VaultTransitKeyExportDetail exportKey(String keyName, VaultTransitExportKeyType keyType, String keyVersion) {
        return engine.exportKey(keyName, keyType, keyVersion).await().indefinitely();
    }

    /**
     * Read the configuration of a Transit key.
     *
     * @param keyName key name
     * @return key detail, or null if the key does not exist
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#read-key">read key</a>
     */
    public Optional<VaultTransitKeyDetail<?>> readKey(String keyName) {
        return engine.readKey(keyName).await().indefinitely();
    }

    /**
     * List all Transit keys.
     *
     * @return key names
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#list-keys">list keys</a>
     */
    public List<String> listKeys() {
        return engine.listKeys().await().indefinitely();
    }
}

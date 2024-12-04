package io.quarkus.vault;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import io.quarkus.vault.transit.VaultTransitDataKey;
import io.quarkus.vault.transit.VaultTransitDataKeyRequestDetail;
import io.quarkus.vault.transit.VaultTransitDataKeyType;
import io.quarkus.vault.transit.VaultTransitExportKeyType;
import io.quarkus.vault.transit.VaultTransitKeyDetail;
import io.quarkus.vault.transit.VaultTransitKeyExportDetail;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;
import io.smallrye.mutiny.Uni;

/**
 * A service that interacts with Hashicorp's Vault Transit secret engine to encrypt, decrypt and sign arbitrary data.
 *
 * @see <a href="https://www.vaultproject.io/docs/secrets/transit/index.html#transit-secrets-engine">Transit Secrets Engine</a>
 */
public interface VaultTransitSecretReactiveEngine {

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
    Uni<String> encrypt(String keyName, String clearData);

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
    Uni<String> encrypt(String keyName, ClearData clearData, TransitContext transitContext);

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
    Uni<Map<EncryptionRequest, String>> encrypt(String keyName, List<EncryptionRequest> requests);

    /**
     * Decrypt the encrypted data with the specified key, and return unencrypted data.
     *
     * @param keyName the key that was used to encrypt the original data
     * @param ciphertext the encrypted data
     * @return the unencrypted data
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#decrypt-data">decrypt data</a>
     */
    Uni<ClearData> decrypt(String keyName, String ciphertext);

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
    Uni<ClearData> decrypt(String keyName, String ciphertext, TransitContext transitContext);

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
    Uni<Map<DecryptionRequest, ClearData>> decrypt(String keyName, List<DecryptionRequest> requests);

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
    Uni<String> rewrap(String keyName, String ciphertext);

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
    Uni<String> rewrap(String keyName, String ciphertext, TransitContext transitContext);

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
    Uni<Map<RewrappingRequest, String>> rewrap(String keyName, List<RewrappingRequest> requests);

    /**
     * Sign an input string with the specified key.
     *
     * @param keyName the signing key to use
     * @param input String to sign
     * @return the signature
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    Uni<String> sign(String keyName, String input);

    /**
     * Sign the input with the specified key and an optional transit context used for key derivation, if applicable.
     *
     * @param keyName the signing key to use
     * @param input data to sign
     * @param transitContext optional transit context used for key derivation
     * @return the signature
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#sign-data">sign data</a>
     */
    Uni<String> sign(String keyName, SigningInput input, TransitContext transitContext);

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
    Uni<String> sign(String keyName, SigningInput input, SignVerifyOptions options, TransitContext transitContext);

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
    Uni<Map<SigningRequest, String>> sign(String keyName, List<SigningRequest> requests);

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
    Uni<Map<SigningRequest, String>> sign(String keyName, List<SigningRequest> requests, SignVerifyOptions options);

    /**
     * Checks that the signature was obtained from signing the input with the specified key.
     * The service will throw a {@link VaultException} if this is not the case.
     *
     * @param keyName the key that was used to sign the input
     * @param signature the signature obtained from one of the sign methods
     * @param input the original input data
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    Uni<Void> verifySignature(String keyName, String signature, String input);

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
    Uni<Void> verifySignature(String keyName, String signature, SigningInput input, TransitContext transitContext);

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
    Uni<Void> verifySignature(String keyName, String signature, SigningInput input, SignVerifyOptions options,
            TransitContext transitContext);

    /**
     * Checks a list of verification requests. Each request shall specify an input and the signature we want to match
     * against, and an optional transit context used for key derivation, if applicable. If the signature does not
     * match, or if any other error occurs, the service will throw a {@link VaultVerificationBatchException}
     *
     * @param keyName the key that was used to sign the input
     * @param requests a list of items specifying an input and a signature to match against
     * @see <a href="https://www.vaultproject.io/api/secret/transit/index.html#verify-signed-data">verify signed data</a>
     */
    Uni<Void> verifySignature(String keyName, List<VerificationRequest> requests);

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
    Uni<Void> verifySignature(String keyName, List<VerificationRequest> requests, SignVerifyOptions options);

    // --- admin operations

    /**
     * Create a new Transit key.
     *
     * @param keyName key name
     * @param detail key creation detail or null
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#create-key">create key</a>
     */
    Uni<Void> createKey(String keyName, KeyCreationRequestDetail detail);

    /**
     * Update the configuration of a Transit key. The key must exist.
     *
     * @param keyName key name
     * @param detail key configuration detail
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#update-key-configuration">update key configuration</a>
     */
    Uni<Void> updateKeyConfiguration(String keyName, KeyConfigRequestDetail detail);

    /**
     * This endpoint rotates the version of the named key.
     * After rotation, new plaintext requests will be encrypted with the new version of the key.
     *
     * @param keyName the key name
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#rotate-key">rotate key</a>
     */
    Uni<Void> rotateKey(String keyName);

    /**
     * Delete a Transit key. Key must have been configured with deletion allowed. The key must exist.
     *
     * @param keyName key name
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#delete-key">delete key</a>
     */
    Uni<Void> deleteKey(String keyName);

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
    Uni<VaultTransitKeyExportDetail> exportKey(String keyName, VaultTransitExportKeyType keyType, String keyVersion);

    /**
     * Read the configuration of a Transit key.
     *
     * @param keyName key name
     * @return key detail, or null if the key does not exist
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#read-key">read key</a>
     */
    Uni<Optional<VaultTransitKeyDetail<?>>> readKey(String keyName);

    /**
     * List all Transit keys.
     *
     * @return key names
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#list-keys">list keys</a>
     */
    Uni<List<String>> listKeys();

    /**
     * Create a data key
     *
     * @param type plaintext or wrapped
     * @param keyName the key name
     * @param detail optional context, nonce and bits
     * @return the generated data key
     * @see <a href="https://www.vaultproject.io/api-docs/secret/transit#generate-data-key">Generate Data Key</a>
     */
    Uni<VaultTransitDataKey> generateDataKey(VaultTransitDataKeyType type, String keyName,
            VaultTransitDataKeyRequestDetail detail);
}

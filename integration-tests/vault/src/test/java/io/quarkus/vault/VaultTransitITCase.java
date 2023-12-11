package io.quarkus.vault;

import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_DERIVED_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY2_NAME;
import static io.quarkus.vault.test.VaultTestExtension.ENCRYPTION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_DERIVATION_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY2_NAME;
import static io.quarkus.vault.test.VaultTestExtension.SIGN_KEY_NAME;
import static io.quarkus.vault.test.VaultTestExtension.TRANSIT_ENGINE_CUSTOM_MOUNT_PATH;
import static io.quarkus.vault.transit.VaultTransitExportKeyType.encryption;
import static io.quarkus.vault.transit.VaultTransitSecretEngineConstants.INVALID_SIGNATURE;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.test.QuarkusUnitTest;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.vault.runtime.VaultAuthManager;
import io.quarkus.vault.test.VaultTestLifecycleManager;
import io.quarkus.vault.test.client.TestVaultClient;
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
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyVersion;
import io.quarkus.vault.transit.VaultTransitKeyDetail;
import io.quarkus.vault.transit.VaultTransitKeyExportDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyVersion;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;

@DisabledOnOs(OS.WINDOWS) // https://github.com/quarkusio/quarkus/issues/3796
@QuarkusTestResource(VaultTestLifecycleManager.class)
public class VaultTransitITCase {
    private static final Logger log = Logger.getLogger(VaultTransitITCase.class);

    public static final String COUCOU = "coucou";
    public static final String NEW_KEY = "new-key";

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("application-vault.properties", "application.properties"));
    public static final String KEY_NAME = "mykey";

    private TransitContext context = TransitContext.fromContext("my context");
    private ClearData data = new ClearData(COUCOU);
    private SigningInput input = new SigningInput(COUCOU);

    @Inject
    VaultTransitSecretEngine transitSecretEngine;

    @Inject
    VaultTransitSecretEngineFactory transitSecretEngineFactory;

    @Inject
    VaultAuthManager vaultAuthManager;

    private final Supplier<VaultTransitSecretEngine> transitSecretEngineWithCustomPath = () ->
            transitSecretEngineFactory.engine(TRANSIT_ENGINE_CUSTOM_MOUNT_PATH);

    private List<VaultTransitSecretEngine> getTransitEngines() {
        return Arrays.asList(transitSecretEngine, transitSecretEngineWithCustomPath.get());
    }

    @Test
    public void encryptionString() {
        getTransitEngines().forEach(engine -> {
            String ciphertext = engine.encrypt(ENCRYPTION_KEY_NAME, COUCOU);
            ClearData decrypted = engine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
            assertEquals(COUCOU, decrypted.asString());
        });
    }

    @Test
    public void encryptionBytes() {
        getTransitEngines().forEach(engine -> {
            String ciphertext = engine.encrypt(ENCRYPTION_KEY_NAME, data, null);
            ClearData decrypted = engine.decrypt(ENCRYPTION_KEY_NAME, ciphertext, null);
            assertEquals(COUCOU, decrypted.asString());
        });
    }

    @Test
    public void encryptionContext() {
        getTransitEngines().forEach(engine -> {
            String ciphertext = engine.encrypt(ENCRYPTION_DERIVED_KEY_NAME, data, context);
            ClearData decrypted = engine.decrypt(ENCRYPTION_DERIVED_KEY_NAME, ciphertext, context);
            assertEquals(COUCOU, decrypted.asString());
        });
    }

    @Test
    public void encryptionBatch() {
        getTransitEngines().forEach(engine -> {
            List<EncryptionRequest> encryptBatch = singletonList(new EncryptionRequest(data));
            Map<EncryptionRequest, String> encryptList = engine.encrypt(ENCRYPTION_KEY_NAME, encryptBatch);
            String ciphertext = getSingleValue(encryptList);
            List<DecryptionRequest> decryptBatch = singletonList(new DecryptionRequest(ciphertext));
            Map<DecryptionRequest, ClearData> decryptList = engine.decrypt(ENCRYPTION_KEY_NAME, decryptBatch);
            assertEquals(1, decryptList.size());
            assertEquals(COUCOU, getSingleValue(decryptList).asString());
        });
    }

    @Test
    public void rewrapBatch() {
        getTransitEngines().forEach(engine -> {
            String ciphertext = engine.encrypt(ENCRYPTION_KEY_NAME, COUCOU);
            ClearData decrypted = engine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
            assertEquals(COUCOU, decrypted.asString());

            List<RewrappingRequest> rewrapBatch = singletonList(new RewrappingRequest(ciphertext));
            Map<RewrappingRequest, String> rewrapBatchResult = engine.rewrap(ENCRYPTION_KEY_NAME, rewrapBatch);
            ciphertext = getSingleValue(rewrapBatchResult);

            decrypted = engine.decrypt(ENCRYPTION_KEY_NAME, ciphertext);
            assertEquals(COUCOU, decrypted.asString());
        });
    }

    @Test
    public void upsert() {
        getTransitEngines().forEach(engine -> {
            String ciphertext = engine.encrypt(NEW_KEY, data, null);
            ClearData decrypted = engine.decrypt(NEW_KEY, ciphertext, null);
            assertEquals(COUCOU, decrypted.asString());
        });
    }

    @Test
    public void signString() {
        getTransitEngines().forEach(engine -> {
            String signature = engine.sign(SIGN_KEY_NAME, input, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, null);
        });
    }

    @Test
    public void signStringExplicitHashAlgorithmSha256() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setHashAlgorithm("sha2-256");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signStringExplicitHashAlgorithmSha512() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setHashAlgorithm("sha2-512");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signStringExplicitHashAlgorithmMismatched() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setHashAlgorithm("sha2-256");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            assertThrows(VaultException.class,
                    () -> engine.verifySignature(SIGN_KEY_NAME, signature, input,
                            options.setHashAlgorithm("sha1"), null));
        });
    }

    @Test
    public void signStringExplicitMarshalingAlgorithmASN1() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setMarshalingAlgorithm("asn1");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signStringExplicitMarshalingAlgorithmJWS() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setMarshalingAlgorithm("jws");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signStringExplicitMarshalingAlgorithmMismatched() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setMarshalingAlgorithm("jws");
            String signature = engine.sign(SIGN_KEY_NAME, input, options, null);
            assertThrows(VaultException.class,
                    () -> engine.verifySignature(SIGN_KEY_NAME, signature, input,
                            options.setMarshalingAlgorithm("asn1"), null));
        });
    }

    @Test
    public void signStringExplicitSignatureAlgorithmPKCS1() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setSignatureAlgorithm("pkcs1v15");
            String signature = engine.sign(SIGN_KEY2_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY2_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signStringExplicitSignatureAlgorithmPSS() {
        getTransitEngines().forEach(engine -> {
            SignVerifyOptions options = new SignVerifyOptions().setSignatureAlgorithm("pss");
            String signature = engine.sign(SIGN_KEY2_NAME, input, options, null);
            engine.verifySignature(SIGN_KEY2_NAME, signature, input, options, null);
        });
    }

    @Test
    public void signJws() {
        getTransitEngines().forEach(engine -> {
            String signature = engine.sign("jws", input, null);
            engine.verifySignature("jws", signature, input, null);
        });
    }

    @Test
    public void signBytes() {
        getTransitEngines().forEach(engine -> {
            String signature = engine.sign(SIGN_KEY_NAME, input, null);
            engine.verifySignature(SIGN_KEY_NAME, signature, input, null);
        });
    }

    @Test
    public void signContext() {
        getTransitEngines().forEach(engine -> {
            String signature = engine.sign(SIGN_DERIVATION_KEY_NAME, input, context);
            engine.verifySignature(SIGN_DERIVATION_KEY_NAME, signature, input, context);
        });
    }


    @Test
    public void signBatch() {
        getTransitEngines().forEach(engine -> {
            List<SigningRequest> batch = singletonList(new SigningRequest(input));
            Map<SigningRequest, String> signatures = engine.sign(SIGN_KEY_NAME, batch);
            assertEquals(1, signatures.size());
            String signature = getSingleValue(signatures);
            List<VerificationRequest> batchVerify = singletonList(new VerificationRequest(signature, input));
            engine.verifySignature(SIGN_KEY_NAME, batchVerify);
        });
    }

    @Test
    public void keyVersionEncryption() {
        getTransitEngines().forEach(engine -> {
            rotate(engine.getMount(), ENCRYPTION_KEY2_NAME);

            String encryptV1 = encrypt(engine, 1);
            assertTrue(encryptV1.startsWith("vault:v1"));
            assertEquals(COUCOU, decrypt(engine, encryptV1));

            String rewraped = engine.rewrap(ENCRYPTION_KEY2_NAME, encryptV1, null);
            assertTrue(rewraped.startsWith("vault:v2"));

            String encryptV2 = encrypt(engine, 2);
            assertTrue(encryptV2.startsWith("vault:v2"));
            assertEquals(COUCOU, decrypt(engine, encryptV2));
        });
    }

    private void rotate(String mount, String keyName) {
        TestVaultClient client = new TestVaultClient();
        String clientToken = vaultAuthManager.getClientToken(client).await().indefinitely();
        client.rotate(mount, clientToken, keyName).await().indefinitely();
    }

    private String encrypt(VaultTransitSecretEngine engine, int keyVersion) {
        EncryptionRequest request = new EncryptionRequest(data, keyVersion);
        List<EncryptionRequest> encryptBatch = singletonList(request);
        Map<EncryptionRequest, String> encryptList = engine.encrypt(ENCRYPTION_KEY2_NAME, encryptBatch);
        String ciphertext = getSingleValue(encryptList);
        return ciphertext;
    }

    private String decrypt(VaultTransitSecretEngine engine, String ciphertext) {
        DecryptionRequest request = new DecryptionRequest(ciphertext);
        List<DecryptionRequest> decryptBatch = singletonList(request);
        Map<DecryptionRequest, ClearData> decryptList = engine.decrypt(ENCRYPTION_KEY2_NAME, decryptBatch);
        return getSingleValue(decryptList).asString();
    }

    @Test
    public void keyVersionSign() {
        getTransitEngines().forEach(engine -> {
            rotate(engine.getMount(), SIGN_KEY2_NAME);

            String sign1 = sign(engine, 1);
            assertTrue(sign1.startsWith("vault:v1"));
            engine.verifySignature(SIGN_KEY2_NAME, sign1, input, null);

            String sign2 = sign(engine, 2);
            assertTrue(sign2.startsWith("vault:v2"));
            engine.verifySignature(SIGN_KEY2_NAME, sign2, input, null);
        });
    }

    @Test
    public void keyVersionSignBatch() {
        getTransitEngines().forEach(engine -> {
            SigningRequest signingRequest1 = new SigningRequest(input, 1);
            SigningRequest signingRequest2 = new SigningRequest(input, 2);
            List<SigningRequest> signingRequests = Arrays.asList(signingRequest1, signingRequest2);

            Map<SigningRequest, String> signatures = engine.sign(SIGN_KEY2_NAME, signingRequests);

            assertEquals(2, signatures.size());
            String sign1 = signatures.get(signingRequest1);
            String sign2 = signatures.get(signingRequest2);
            assertTrue(sign1.startsWith("vault:v1"));
            assertTrue(sign2.startsWith("vault:v2"));

            VerificationRequest verificationRequest1 = new VerificationRequest(sign1, input);
            VerificationRequest verificationRequest2 = new VerificationRequest(sign2, input);
            List<VerificationRequest> verificationRequests = Arrays.asList(verificationRequest1, verificationRequest2);

            engine.verifySignature(SIGN_KEY2_NAME, verificationRequests);
        });
    }

    private String sign(VaultTransitSecretEngine engine, int keyVersion) {
        SigningRequest request = new SigningRequest(input, keyVersion);
        Map<SigningRequest, String> signingResults = engine.sign(SIGN_KEY2_NAME, singletonList(request));
        String signature = getSingleValue(signingResults);
        return signature;
    }

    @Test
    public void verifySignatureInvalid() {
        getTransitEngines().forEach(engine -> {
            String signature = engine.sign(SIGN_KEY_NAME, input, null);
            SigningInput otherInput = new SigningInput("some other input");

            try {
                engine.verifySignature(SIGN_KEY_NAME, signature, otherInput, null);
                fail();
            } catch (VaultException e) {
                assertEquals(INVALID_SIGNATURE, e.getMessage());
            }

            VerificationRequest request = new VerificationRequest(signature, otherInput);
            try {
                engine.verifySignature(SIGN_KEY_NAME, List.of(request));
                fail();
            } catch (VaultVerificationBatchException e) {
                assertTrue(e.getValid().isEmpty());
                assertEquals(1, e.getErrors().size());
                assertEquals(INVALID_SIGNATURE, e.getErrors().get(request));
            }
        });
    }

    @Test
    public void bigSignBatch() {
        getTransitEngines().forEach(engine -> {
            List<SigningRequest> signingRequests = IntStream.range(0, 1000)
                    .mapToObj(i -> new SigningRequest(new SigningInput("coucou" + i)))
                    .collect(toList());

            Map<SigningRequest, String> signatures = engine.sign(SIGN_KEY_NAME, signingRequests);

            List<VerificationRequest> verificationRequests = signatures.entrySet().stream()
                    .map(e -> new VerificationRequest(e.getValue(), e.getKey().getInput()))
                    .collect(toList());

            engine.verifySignature(SIGN_KEY_NAME, verificationRequests);
        });
    }

    private <K, V> V getSingleValue(Map<K, V> map) {
        assertEquals(1, map.size());
        return map.values().stream().findFirst().get();
    }

    @Test
    public void adminKey() {
        getTransitEngines().forEach(engine -> {
            assertFalse(engine.listKeys().contains(KEY_NAME));
            engine.createKey(KEY_NAME, new KeyCreationRequestDetail().setExportable(true));
            assertTrue(engine.listKeys().contains(KEY_NAME));

            VaultTransitKeyDetail<?> mykey = engine.readKey(KEY_NAME).get();
            assertEquals(KEY_NAME, mykey.getName());
            assertTrue(mykey.isExportable());
            assertFalse(mykey.isDeletionAllowed());
            assertTrue(mykey.isSupportsDecryption());
            assertTrue(mykey.isSupportsEncryption());
            assertTrue(mykey.isSupportsDerivation());
            assertEquals(1, mykey.getKeys().size());
            assertTrue(mykey.getKeys().containsKey("1"));
            assertEquals(1, mykey.getVersions().size());
            assertTrue(mykey.getVersions().containsKey("1"));
            assertEquals(1, mykey.getMinDecryptionVersion());
            assertEquals(0, mykey.getMinEncryptionVersion());

            VaultTransitKeyExportDetail exportDetail = engine.exportKey(KEY_NAME, encryption, "1");
            assertEquals(KEY_NAME, exportDetail.getName());
            assertEquals(1, exportDetail.getKeys().size());
            assertTrue(exportDetail.getKeys().containsKey("1"));

            engine.updateKeyConfiguration(KEY_NAME, new KeyConfigRequestDetail().setDeletionAllowed(true));
            mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey.isDeletionAllowed());

            engine.deleteKey(KEY_NAME);
            assertTrue(engine.readKey(KEY_NAME).isEmpty());
        });
    }

    @Test
    public void asymmetricReadECDSAKey() {
        getTransitEngines().forEach(engine -> {
            assertFalse(engine.listKeys().contains(KEY_NAME));
            engine.createKey(KEY_NAME, new KeyCreationRequestDetail().setType("ecdsa-p256"));
            assertTrue(engine.listKeys().contains(KEY_NAME));

            VaultTransitKeyDetail<?> mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey instanceof VaultTransitAsymmetricKeyDetail);
            assertEquals(KEY_NAME, mykey.getName());
            assertFalse(mykey.isExportable());
            assertFalse(mykey.isDeletionAllowed());
            assertFalse(mykey.isSupportsDecryption());
            assertFalse(mykey.isSupportsEncryption());
            assertFalse(mykey.isSupportsDerivation());
            assertTrue(mykey.isSupportsSigning());
            assertEquals(mykey.getType(), "ecdsa-p256");
            assertEquals(1, mykey.getKeys().size());
            assertTrue(mykey.getKeys().containsKey("1"));
            assertEquals(1, mykey.getVersions().size());
            assertTrue(mykey.getVersions().containsKey("1"));
            assertNotNull(mykey.getVersions().get("1").getCreationTime());
            assertTrue(mykey.getVersions().get("1") instanceof VaultTransitAsymmetricKeyVersion);
            assertNotNull(((VaultTransitAsymmetricKeyVersion) mykey.getVersions().get("1")).getPublicKey());
            assertEquals(1, mykey.getLatestVersion());
            assertEquals(0, mykey.getMinAvailableVersion());
            assertEquals(1, mykey.getMinDecryptionVersion());
            assertEquals(0, mykey.getMinEncryptionVersion());

            engine.updateKeyConfiguration(KEY_NAME, new KeyConfigRequestDetail().setDeletionAllowed(true));
            mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey.isDeletionAllowed());

            engine.deleteKey(KEY_NAME);
            assertTrue(engine.readKey(KEY_NAME).isEmpty());
        });
    }

    @Test
    public void asymmetricReadRSAKey() {
        getTransitEngines().forEach(engine -> {
            assertFalse(engine.listKeys().contains(KEY_NAME));
            engine.createKey(KEY_NAME, new KeyCreationRequestDetail().setType("rsa-2048"));
            assertTrue(engine.listKeys().contains(KEY_NAME));

            VaultTransitKeyDetail<?> mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey instanceof VaultTransitAsymmetricKeyDetail);
            assertEquals(KEY_NAME, mykey.getName());
            assertFalse(mykey.isExportable());
            assertFalse(mykey.isDeletionAllowed());
            assertTrue(mykey.isSupportsDecryption());
            assertTrue(mykey.isSupportsEncryption());
            assertFalse(mykey.isSupportsDerivation());
            assertTrue(mykey.isSupportsSigning());
            assertEquals("rsa-2048", mykey.getType());
            assertEquals(1, mykey.getKeys().size());
            assertTrue(mykey.getKeys().containsKey("1"));
            assertEquals(1, mykey.getVersions().size());
            assertTrue(mykey.getVersions().containsKey("1"));
            assertNotNull(mykey.getVersions().get("1").getCreationTime());
            assertTrue(mykey.getVersions().get("1") instanceof VaultTransitAsymmetricKeyVersion);
            assertNotNull(((VaultTransitAsymmetricKeyVersion) mykey.getVersions().get("1")).getPublicKey());
            assertEquals(1, mykey.getLatestVersion());
            assertEquals(0, mykey.getMinAvailableVersion());
            assertEquals(1, mykey.getMinDecryptionVersion());
            assertEquals(0, mykey.getMinEncryptionVersion());

            engine.updateKeyConfiguration(KEY_NAME, new KeyConfigRequestDetail().setDeletionAllowed(true));
            mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey.isDeletionAllowed());

            engine.deleteKey(KEY_NAME);
            assertTrue(engine.readKey(KEY_NAME).isEmpty());
        });
    }

    @Test
    public void symmetricReadAESKey() {
        getTransitEngines().forEach(engine -> {
            assertFalse(engine.listKeys().contains(KEY_NAME));
            engine.createKey(KEY_NAME, new KeyCreationRequestDetail().setType("aes256-gcm96"));
            assertTrue(engine.listKeys().contains(KEY_NAME));

            VaultTransitKeyDetail<?> mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey instanceof VaultTransitSymmetricKeyDetail);
            assertEquals(KEY_NAME, mykey.getName());
            assertFalse(mykey.isExportable());
            assertFalse(mykey.isDeletionAllowed());
            assertTrue(mykey.isSupportsDecryption());
            assertTrue(mykey.isSupportsEncryption());
            assertTrue(mykey.isSupportsDerivation());
            assertFalse(mykey.isSupportsSigning());
            assertEquals(mykey.getType(), "aes256-gcm96");
            assertEquals(1, mykey.getKeys().size());
            assertTrue(mykey.getKeys().containsKey("1"));
            assertEquals(1, mykey.getVersions().size());
            assertTrue(mykey.getVersions().containsKey("1"));
            assertNotNull(mykey.getVersions().get("1").getCreationTime());
            assertTrue(mykey.getVersions().get("1") instanceof VaultTransitSymmetricKeyVersion);
            assertEquals(1, mykey.getLatestVersion());
            assertEquals(0, mykey.getMinAvailableVersion());
            assertEquals(1, mykey.getMinDecryptionVersion());
            assertEquals(0, mykey.getMinEncryptionVersion());

            engine.updateKeyConfiguration(KEY_NAME, new KeyConfigRequestDetail().setDeletionAllowed(true));
            mykey = engine.readKey(KEY_NAME).get();
            assertTrue(mykey.isDeletionAllowed());

            engine.deleteKey(KEY_NAME);
            assertTrue(engine.readKey(KEY_NAME).isEmpty());
        });
    }
}

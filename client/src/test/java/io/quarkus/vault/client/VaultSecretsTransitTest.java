package io.quarkus.vault.client;

import static io.quarkus.vault.client.api.common.VaultHashAlgorithm.SHA2_512;
import static io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitExportKeyType.*;
import static io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitKeyType.*;
import static io.quarkus.vault.client.api.secrets.transit.VaultSecretsTransitSignatureAlgorithm.PKCS1V15;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.io.StringReader;
import java.security.*;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.testcontainers.shaded.org.bouncycastle.openssl.PEMParser;

import io.quarkus.vault.client.api.common.VaultFormat;
import io.quarkus.vault.client.api.common.VaultRandomSource;
import io.quarkus.vault.client.api.secrets.transit.*;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(secrets = {
        @Mount(type = "transit", path = "transit")
})
public class VaultSecretsTransitTest {

    static final Base64.Decoder B64_DEC = Base64.getDecoder();
    static final Base64.Encoder B64_ENC = Base64.getEncoder();
    static final HexFormat HEX = HexFormat.of();

    @Test
    public void testCreateKey(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var key = transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        assertThat(key)
                .isNotNull()
                .returns(false, VaultSecretsTransitKeyInfo::isAllowPlaintextBackup)
                .returns(0L, VaultSecretsTransitKeyInfo::getAutoRotatePeriod)
                .returns(false, VaultSecretsTransitKeyInfo::isDeletionAllowed)
                .returns(false, VaultSecretsTransitKeyInfo::isDerived)
                .returns(false, VaultSecretsTransitKeyInfo::isExportable)
                .returns(false, VaultSecretsTransitKeyInfo::isImportedKey)
                .returns(1, VaultSecretsTransitKeyInfo::getLatestVersion)
                .returns(0, VaultSecretsTransitKeyInfo::getMinAvailableVersion)
                .returns(1, VaultSecretsTransitKeyInfo::getMinDecryptionVersion)
                .returns(0, VaultSecretsTransitKeyInfo::getMinEncryptionVersion)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsDecryption)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsDerivation)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsEncryption)
                .returns(true, VaultSecretsTransitKeyInfo::isSupportsSigning)
                .returns(ECDSA_P256, VaultSecretsTransitKeyInfo::getType);

        assertThat(key.getKeys())
                .containsKeys("1");

        var keyVersion = key.getKeys().get("1");

        assertThat(keyVersion.getName())
                .isEqualTo("P-256");
        assertThat(keyVersion.getPublicKey())
                .isNotEmpty();
        assertThat(keyVersion.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(keyVersion.getCertificateChain())
                .isEmpty();
    }

    @Test
    public void testReadKey(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var key = transitApi.readKey(keyName)
                .await().indefinitely();

        assertThat(key)
                .isNotNull()
                .returns(false, VaultSecretsTransitKeyInfo::isAllowPlaintextBackup)
                .returns(0L, VaultSecretsTransitKeyInfo::getAutoRotatePeriod)
                .returns(false, VaultSecretsTransitKeyInfo::isDeletionAllowed)
                .returns(false, VaultSecretsTransitKeyInfo::isDerived)
                .returns(false, VaultSecretsTransitKeyInfo::isExportable)
                .returns(false, VaultSecretsTransitKeyInfo::isImportedKey)
                .returns(1, VaultSecretsTransitKeyInfo::getLatestVersion)
                .returns(0, VaultSecretsTransitKeyInfo::getMinAvailableVersion)
                .returns(1, VaultSecretsTransitKeyInfo::getMinDecryptionVersion)
                .returns(0, VaultSecretsTransitKeyInfo::getMinEncryptionVersion)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsDecryption)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsDerivation)
                .returns(false, VaultSecretsTransitKeyInfo::isSupportsEncryption)
                .returns(true, VaultSecretsTransitKeyInfo::isSupportsSigning)
                .returns(ECDSA_P256, VaultSecretsTransitKeyInfo::getType);

        assertThat(key.getKeys())
                .containsKeys("1");

        var keyVersion = key.getKeys().get("1");

        assertThat(keyVersion.getName())
                .isEqualTo("P-256");
        assertThat(keyVersion.getPublicKey())
                .isNotEmpty();
        assertThat(keyVersion.getCreationTime())
                .isBetween(now().minusSeconds(1), now().plusSeconds(1));
        assertThat(keyVersion.getCertificateChain())
                .isEmpty();
    }

    @Test
    public void testListKeys(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var keys = transitApi.listKeys()
                .await().indefinitely();

        assertThat(keys)
                .isNotNull()
                .contains(keyName);
    }

    @Test
    public void testDeleteKey(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var key = transitApi.createKey(keyName, null)
                .await().indefinitely();

        assertThat(key)
                .isNotNull();

        transitApi.updateKey(keyName, new VaultSecretsTransitUpdateKeyParams()
                .setDeletionAllowed(true))
                .await().indefinitely();

        transitApi.deleteKey(keyName)
                .await().indefinitely();

        var keys = transitApi.listKeys()
                .await().indefinitely();

        assertThat(keys)
                .doesNotContain(keyName);
    }

    @Test
    public void testRotateKey(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var key = transitApi.createKey(keyName, null)
                .await().indefinitely();

        assertThat(key)
                .isNotNull();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var rotatedKey = transitApi.readKey(keyName)
                .await().indefinitely();

        assertThat(rotatedKey)
                .isNotNull()
                .returns(2, VaultSecretsTransitKeyInfo::getLatestVersion);
    }

    @Test
    public void testEncryptDecrypt(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);

        var encrypted = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext))
                .await().indefinitely();

        assertThat(encrypted)
                .isNotNull();
        assertThat(encrypted.getCiphertext())
                .isNotEmpty();
        assertThat(encrypted.getKeyVersion())
                .isEqualTo(1);

        var decrypted = transitApi.decrypt(keyName, new VaultSecretsTransitDecryptParams()
                .setCiphertext(encrypted.getCiphertext()))
                .await().indefinitely();

        assertThat(decrypted)
                .isEqualTo(plaintext);
    }

    @Test
    public void testEncryptDecryptWithDerivation(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setDerived(true))
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);

        var context = client.sys().tools().random(16, null, null)
                .await().indefinitely();

        var encrypted = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext)
                .setContext(context))
                .await().indefinitely();

        assertThat(encrypted)
                .isNotNull();
        assertThat(encrypted.getCiphertext())
                .isNotEmpty();
        assertThat(encrypted.getKeyVersion())
                .isEqualTo(1);

        var decrypted = transitApi.decrypt(keyName, new VaultSecretsTransitDecryptParams()
                .setCiphertext(encrypted.getCiphertext())
                .setContext(context))
                .await().indefinitely();

        assertThat(decrypted)
                .isEqualTo(plaintext);
    }

    @Test
    public void testEncryptDecryptWithConvergentAndDerivation(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setConvergentEncryption(true)
                .setDerived(true))
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);

        var context = transitApi.random(16, null, null)
                .await().indefinitely();

        var encrypted = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext)
                .setContext(context))
                .await().indefinitely();

        assertThat(encrypted)
                .isNotNull();
        assertThat(encrypted.getCiphertext())
                .isNotEmpty();
        assertThat(encrypted.getKeyVersion())
                .isEqualTo(1);

        var decrypted = transitApi.decrypt(keyName, new VaultSecretsTransitDecryptParams()
                .setCiphertext(encrypted.getCiphertext())
                .setContext(context))
                .await().indefinitely();

        assertThat(decrypted)
                .isEqualTo(plaintext);
    }

    @Test
    public void testEncryptDecryptWithAD(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);
        var associatedData = "something-else".getBytes(UTF_8);

        var encrypted = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext)
                .setAssociatedData(associatedData))
                .await().indefinitely();

        assertThat(encrypted)
                .isNotNull();
        assertThat(encrypted.getCiphertext())
                .isNotEmpty();
        assertThat(encrypted.getKeyVersion())
                .isEqualTo(1);

        var decrypted = transitApi.decrypt(keyName, new VaultSecretsTransitDecryptParams()
                .setCiphertext(encrypted.getCiphertext())
                .setAssociatedData(associatedData))
                .await().indefinitely();

        assertThat(decrypted)
                .isEqualTo(plaintext);
    }

    @Test
    public void testEncryptWithConvergent(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setConvergentEncryption(true)
                .setDerived(true))
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);

        var context = client.sys().tools().random(16, null, null)
                .await().indefinitely();

        var encrypted1 = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext)
                .setContext(context))
                .await().indefinitely();

        assertThat(encrypted1)
                .isNotNull();
        assertThat(encrypted1.getCiphertext())
                .isNotEmpty();

        var encrypted2 = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext)
                .setContext(context))
                .await().indefinitely();

        assertThat(encrypted2)
                .isNotNull();
        assertThat(encrypted2.getCiphertext())
                .isNotEmpty();

        assertThat(encrypted2.getCiphertext())
                .isEqualTo(encrypted1.getCiphertext());
    }

    @Test
    public void testBatchEncryptDecrypt(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setReference("item1"),
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2))))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var result1 = encrypted.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();
        assertThat(result1.getKeyVersion())
                .isEqualTo(1);
        assertThat(result1.getReference())
                .isEqualTo("item1");

        var result2 = encrypted.get(1);

        assertThat(result2.getCiphertext())
                .isNotEmpty();
        assertThat(result2.getKeyVersion())
                .isEqualTo(1);
        assertThat(result2.getReference())
                .isEmpty();

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result1.getCiphertext())
                                .setReference("item1"),
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result2.getCiphertext())
                                .setReference("item2"))))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);
        assertThat(decryptedResult1.getReference())
                .isEqualTo("item1");

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEqualTo(plaintext2);
        assertThat(decryptedResult2.getReference())
                .isEqualTo("item2");
    }

    @Test
    public void testBatchEncryptDecryptWithDerivation(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setDerived(true))
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var context1 = transitApi.random(16, null, null)
                .await().indefinitely();
        var context2 = transitApi.random(16, null, null)
                .await().indefinitely();

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setContext(context1),
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setContext(context2))))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var result1 = encrypted.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();

        var result2 = encrypted.get(1);

        assertThat(result2.getCiphertext())
                .isNotEmpty();

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result1.getCiphertext())
                                .setContext(context1),
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result2.getCiphertext())
                                .setContext(context2))))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEqualTo(plaintext2);
    }

    @Test
    public void testBatchEncryptDecryptWithConvergentAndDerivation(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setConvergentEncryption(true)
                .setDerived(true))
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var context1 = transitApi.random(16, null, null)
                .await().indefinitely();
        var context2 = transitApi.random(16, null, null)
                .await().indefinitely();

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setContext(context1),
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setContext(context2))))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var result1 = encrypted.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();

        var result2 = encrypted.get(1);

        assertThat(result2.getCiphertext())
                .isNotEmpty();

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result1.getCiphertext())
                                .setContext(context1),
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result2.getCiphertext())
                                .setContext(context2))))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEqualTo(plaintext2);
    }

    @Test
    public void testBatchEncryptDecryptWithAD(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var associatedData1 = "something-else".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);
        var associatedData2 = "something-completely-else".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setAssociatedData(associatedData1),
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setAssociatedData(associatedData2))))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var result1 = encrypted.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();

        var result2 = encrypted.get(1);

        assertThat(result2.getCiphertext())
                .isNotEmpty();

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .setBatchInput(List.of(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result1.getCiphertext())
                                .setAssociatedData(associatedData1)
                                .setReference("item1"),
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result2.getCiphertext())
                                .setAssociatedData(associatedData2)
                                .setReference("item2"))))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEqualTo(plaintext2);
    }

    @Test
    public void testBatchEncryptDecryptWithKeyVersion(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setKeyVersion(1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setKeyVersion(2)))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var result1 = encrypted.get(0);

        assertThat(result1.getCiphertext())
                .startsWith("vault:v1");
        assertThat(result1.getKeyVersion())
                .isEqualTo(1);

        var result2 = encrypted.get(1);

        assertThat(result2.getCiphertext())
                .startsWith("vault:v2");
        assertThat(result2.getKeyVersion())
                .isEqualTo(2);

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result1.getCiphertext()))
                .addBatchItem(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(result2.getCiphertext())))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEqualTo(plaintext2);
    }

    @Test
    public void testBatchEncryptWithConvergent(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setConvergentEncryption(true)
                .setDerived(true))
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var context1 = client.sys().tools().random(16, null, null)
                .await().indefinitely();
        var context2 = client.sys().tools().random(16, null, null)
                .await().indefinitely();

        var encrypted1 = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setContext(context1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setContext(context2)))
                .await().indefinitely();

        assertThat(encrypted1)
                .hasSize(2);

        var result1 = encrypted1.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();

        var result2 = encrypted1.get(1);

        assertThat(result2.getCiphertext())
                .isNotEmpty();

        var encrypted2 = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1)
                                .setContext(context1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setContext(context2)))
                .await().indefinitely();

        assertThat(encrypted2)
                .hasSize(2);

        var result21 = encrypted2.get(0);

        assertThat(result21.getCiphertext())
                .isEqualTo(result1.getCiphertext());

        var result22 = encrypted2.get(1);

        assertThat(result22.getCiphertext())
                .isEqualTo(result2.getCiphertext());
    }

    @Test
    public void testBatchEncryptWithIndividualErrors(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted1 = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .allowPartialFailure()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)
                                .setKeyVersion(2)))
                .await().indefinitely();

        assertThat(encrypted1)
                .hasSize(2);

        var result1 = encrypted1.get(0);

        assertThat(result1.getCiphertext())
                .isNotEmpty();

        var result2 = encrypted1.get(1);

        assertThat(result2.getCiphertext())
                .isNull();
        assertThat(result2.getError())
                .contains("requested version");
    }

    @Test
    public void testBatchDecryptWithIndividualErrors(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        var decrypted = transitApi.decryptBatch(keyName, new VaultSecretsTransitDecryptBatchParams()
                .allowPartialFailure()
                .addBatchItem(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext(encrypted.get(0).getCiphertext()))
                .addBatchItem(
                        new VaultSecretsTransitDecryptBatchItem()
                                .setCiphertext("junk")))
                .await().indefinitely();

        assertThat(decrypted)
                .hasSize(2);

        var decryptedResult1 = decrypted.get(0);

        assertThat(decryptedResult1.getPlaintext())
                .isEqualTo(plaintext1);
        assertThat(decryptedResult1.getError())
                .isNull();

        var decryptedResult2 = decrypted.get(1);

        assertThat(decryptedResult2.getPlaintext())
                .isEmpty();
        assertThat(decryptedResult2.getError())
                .contains("invalid ciphertext");

    }

    @Test
    public void testRewrap(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext = "test".getBytes(UTF_8);

        var encrypted = transitApi.encrypt(keyName, new VaultSecretsTransitEncryptParams()
                .setPlaintext(plaintext))
                .await().indefinitely();

        assertThat(encrypted)
                .isNotNull();
        assertThat(encrypted.getCiphertext())
                .isNotEmpty();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var rewrapped = transitApi.rewrap(keyName, new VaultSecretsTransitRewrapParams()
                .setKeyVersion(2)
                .setCiphertext(encrypted.getCiphertext()))
                .await().indefinitely();

        assertThat(rewrapped)
                .isNotNull();
        assertThat(rewrapped.getCiphertext())
                .isNotEmpty();
        assertThat(rewrapped.getKeyVersion())
                .isEqualTo(2);
    }

    @Test
    public void testRewrapBatch(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var rewrapped = transitApi.rewrapBatch(keyName, new VaultSecretsTransitRewrapBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitRewrapBatchItem()
                                .setKeyVersion(2)
                                .setCiphertext(encrypted.get(0).getCiphertext()))
                .addBatchItem(
                        new VaultSecretsTransitRewrapBatchItem()
                                .setKeyVersion(3)
                                .setCiphertext(encrypted.get(1).getCiphertext())))
                .await().indefinitely();

        assertThat(rewrapped)
                .hasSize(2);

        var rewrapped1 = rewrapped.get(0);

        assertThat(rewrapped1.getCiphertext())
                .startsWith("vault:v2");
        assertThat(rewrapped1.getKeyVersion())
                .isEqualTo(2);

        var rewrapped2 = rewrapped.get(1);

        assertThat(rewrapped2.getCiphertext())
                .startsWith("vault:v3");
        assertThat(rewrapped2.getKeyVersion())
                .isEqualTo(3);
    }

    @Test
    public void testRewrapBatchWithIndividualErrors(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, null)
                .await().indefinitely();

        var plaintext1 = "hello".getBytes(UTF_8);
        var plaintext2 = "transit".getBytes(UTF_8);

        var encrypted = transitApi.encryptBatch(keyName, new VaultSecretsTransitEncryptBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext1))
                .addBatchItem(
                        new VaultSecretsTransitEncryptBatchItem()
                                .setPlaintext(plaintext2)))
                .await().indefinitely();

        assertThat(encrypted)
                .hasSize(2);

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var rewrapped = transitApi.rewrapBatch(keyName, new VaultSecretsTransitRewrapBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitRewrapBatchItem()
                                .setKeyVersion(2)
                                .setCiphertext(encrypted.get(0).getCiphertext()))
                .addBatchItem(
                        new VaultSecretsTransitRewrapBatchItem()
                                .setKeyVersion(3)
                                .setCiphertext(encrypted.get(1).getCiphertext())))
                .await().indefinitely();

        assertThat(rewrapped)
                .hasSize(2);

        var rewrapped1 = rewrapped.get(0);

        assertThat(rewrapped1.getCiphertext())
                .startsWith("vault:v2");
        assertThat(rewrapped1.getKeyVersion())
                .isEqualTo(2);

        var rewrapped2 = rewrapped.get(1);

        assertThat(rewrapped2.getCiphertext())
                .isNull();
        ;
        assertThat(rewrapped2.getError())
                .contains("requested version");
    }

    @Test
    public void testRandomBase64(VaultClient client) {
        var transitApi = client.secrets().transit();

        var random = transitApi.random(32, VaultRandomSource.ALL, VaultFormat.BASE64)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testRandomHex(VaultClient client) {
        var transitApi = client.secrets().transit();

        var random = transitApi.random(32, VaultRandomSource.PLATFORM, VaultFormat.HEX)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testRandomWithDefaults(VaultClient client) {
        var transitApi = client.secrets().transit();

        var random = transitApi.random(32, null, null)
                .await().indefinitely();

        assertThat(random)
                .isNotNull()
                .hasSize(32);
    }

    @Test
    public void testHash(VaultClient client) throws NoSuchAlgorithmException {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        var hash = transitApi.hash(SHA2_512, data, VaultFormat.BASE64)
                .await().indefinitely();

        var localHash = B64_ENC.encodeToString(MessageDigest.getInstance("SHA-512").digest(data));

        assertThat(hash)
                .isEqualTo(localHash);
    }

    @Test
    public void testHashWithDefaultParams(VaultClient client) throws NoSuchAlgorithmException {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        var hash = transitApi.hash(null, data, null)
                .await().indefinitely();

        var localHash = HEX.formatHex(MessageDigest.getInstance("SHA-256").digest(data));

        assertThat(hash)
                .isEqualTo(localHash);
    }

    @Test
    public void testHmac(VaultClient client, @Random String keyName) throws Exception {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(AES256_GCM96)
                .setExportable(true))
                .await().indefinitely();

        var export = transitApi.exportKey(HMAC_KEY, keyName, "latest")
                .await().indefinitely();

        var hmac = transitApi.hmac(keyName, SHA2_512, data, null)
                .await().indefinitely();

        var mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(B64_DEC.decode(export.getKeys().get("1")), "HmacSHA512"));

        var localHmac = "vault:v1:" + B64_ENC.encodeToString(mac.doFinal(data));

        assertThat(hmac)
                .isEqualTo(localHmac);
    }

    @Test
    public void testHmacBatch(VaultClient client, @Random String keyName) throws Exception {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(AES256_GCM96)
                .setExportable(true))
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var export = transitApi.exportKey(HMAC_KEY, keyName, null)
                .await().indefinitely();

        var hmac = transitApi.hmacBatch(keyName, new VaultSecretsTransitHmacBatchParams()
                .setAlgorithm(SHA2_512)
                .setKeyVersion(2)
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(data1))
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(data2)))
                .await().indefinitely();

        assertThat(hmac)
                .hasSize(2);

        var mac = Mac.getInstance("HmacSHA512");

        mac.init(new SecretKeySpec(B64_DEC.decode(export.getKeys().get("2")), "HmacSHA512"));
        var localHmac1 = "vault:v2:" + B64_ENC.encodeToString(mac.doFinal(data1));

        var hmac1 = hmac.get(0);

        assertThat(hmac1.getHmac())
                .isEqualTo(localHmac1);

        mac.init(new SecretKeySpec(B64_DEC.decode(export.getKeys().get("2")), "HmacSHA512"));
        var localHmac2 = "vault:v2:" + B64_ENC.encodeToString(mac.doFinal(data2));

        var hmac2 = hmac.get(1);

        assertThat(hmac2.getHmac())
                .isEqualTo(localHmac2);
    }

    @Test
    public void testHmacBatchWithIndividualErrors(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(AES256_GCM96)
                .setExportable(true))
                .await().indefinitely();

        var hmac = transitApi.hmacBatch(keyName, new VaultSecretsTransitHmacBatchParams()
                .setAlgorithm(SHA2_512)
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(data1))
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(null)))
                .await().indefinitely();

        assertThat(hmac)
                .hasSize(2);

        var hmac1 = hmac.get(0);

        assertThat(hmac1.getHmac())
                .startsWith("vault:v1:");

        var hmac2 = hmac.get(1);

        assertThat(hmac2.getHmac())
                .isNull();
        assertThat(hmac2.getError())
                .contains("missing input");
    }

    @Test
    public void testSign(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signature = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setHashAlgorithm(SHA2_512))
                .await().indefinitely();

        assertThat(signature)
                .isNotNull();
        assertThat(signature.getSignature())
                .startsWith("vault:v1:");
        assertThat(signature.getPublicKey())
                .isNull();
        assertThat(signature.getKeyVersion())
                .isEqualTo(1);
    }

    @Test
    public void testSignDerived(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        var context = transitApi.random(16, null, null)
                .await().indefinitely();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ED25519)
                .setDerived(true))
                .await().indefinitely();

        var signature = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setContext(context)
                .setHashAlgorithm(SHA2_512))
                .await().indefinitely();

        assertThat(signature)
                .isNotNull();
        assertThat(signature.getSignature())
                .startsWith("vault:v1:");
        assertThat(signature.getPublicKey())
                .isNotEmpty();
        assertThat(signature.getKeyVersion())
                .isEqualTo(1);
    }

    @Test
    public void testSignRSA(VaultClient client, @Random String keyName) throws Exception {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setHashAlgorithm(SHA2_512)
                .setSignatureAlgorithm(PKCS1V15))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();
        assertThat(signResult.getSignature())
                .startsWith("vault:v1:");
        assertThat(signResult.getPublicKey())
                .isNull();
        assertThat(signResult.getKeyVersion())
                .isEqualTo(1);

        var signer = Signature.getInstance("SHA512withRSA");
        signer.initVerify(exportPublicKey(transitApi, keyName, signResult.getKeyVersion(), true));
        signer.update(data);

        var verifyResult = signer.verify(B64_DEC.decode(signResult.getSignature().substring(9)));

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testSignSaltLength(VaultClient client, @Random String keyName) throws Exception {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setSaltLength(23))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();
        assertThat(signResult.getSignature())
                .startsWith("vault:v1:");
        assertThat(signResult.getPublicKey())
                .isNull();
        assertThat(signResult.getKeyVersion())
                .isEqualTo(1);

        var signer = Signature.getInstance("RSASSA-PSS");
        signer.setParameter(new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 23, 1));
        signer.initVerify(exportPublicKey(transitApi, keyName, signResult.getKeyVersion(), true));
        signer.update(data);

        var verifyResult = signer.verify(B64_DEC.decode(signResult.getSignature().substring(9)));

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testSignMarshalAlg(VaultClient client, @Random String keyName) throws Exception {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setMarshalingAlgorithm(VaultSecretsTransitMarshalingAlgorithm.JWS))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();
        assertThat(signResult.getSignature())
                .startsWith("vault:v1:");
        assertThat(signResult.getPublicKey())
                .isNull();
        assertThat(signResult.getKeyVersion())
                .isEqualTo(1);

        var signatureOnly = signResult.getSignature().substring("vault:v1:".length());

        assertThatCode(() -> Base64.getUrlDecoder().decode(signatureOnly))
                .doesNotThrowAnyException();
    }

    @Test
    public void testSignBatch(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var signature = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setHashAlgorithm(SHA2_512)
                .setKeyVersion(2)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signature)
                .hasSize(2);

        var signature1 = signature.get(0);

        assertThat(signature1.getSignature())
                .startsWith("vault:v2:");
        assertThat(signature1.getPublicKey())
                .isNull();
        assertThat(signature1.getKeyVersion())
                .isEqualTo(2);
        assertThat(signature1.getReference())
                .isEqualTo("item1");
        assertThat(signature1.getError())
                .isNull();

        var signature2 = signature.get(1);

        assertThat(signature2.getSignature())
                .startsWith("vault:v2:");
        assertThat(signature2.getPublicKey())
                .isNull();
        assertThat(signature2.getKeyVersion())
                .isEqualTo(2);
        assertThat(signature2.getReference())
                .isEqualTo("item2");
        assertThat(signature2.getError())
                .isNull();
    }

    @Test
    public void testSignBatchDerived(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        var context1 = transitApi.random(16, null, null)
                .await().indefinitely();
        var context2 = transitApi.random(16, null, null)
                .await().indefinitely();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ED25519)
                .setDerived(true))
                .await().indefinitely();

        var signature = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setHashAlgorithm(SHA2_512)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setContext(context1))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setContext(context2)))
                .await().indefinitely();

        assertThat(signature)
                .hasSize(2);

        var signature1 = signature.get(0);

        assertThat(signature1.getSignature())
                .isNotEmpty();
        assertThat(signature1.getPublicKey())
                .isNotEmpty();
        assertThat(signature1.getKeyVersion())
                .isEqualTo(1);

        var signature2 = signature.get(1);

        assertThat(signature2.getSignature())
                .isNotEmpty();
        assertThat(signature2.getPublicKey())
                .isNotEmpty();
        assertThat(signature2.getKeyVersion())
                .isEqualTo(1);
    }

    @Test
    public void testSignBatchWithIndividualErrors(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signature = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setHashAlgorithm(SHA2_512)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signature)
                .hasSize(2);

        var signature1 = signature.get(0);

        assertThat(signature1.getSignature())
                .isNotEmpty();
        assertThat(signature1.getPublicKey())
                .isNull();
        assertThat(signature1.getKeyVersion())
                .isEqualTo(1);
        assertThat(signature1.getReference())
                .isEqualTo("item1");

        var signature2 = signature.get(1);

        assertThat(signature2.getSignature())
                .isNull();
        assertThat(signature2.getPublicKey())
                .isNull();
        assertThat(signature2.getKeyVersion())
                .isEqualTo(0);
        assertThat(signature2.getReference())
                .isEqualTo("item2");
        assertThat(signature2.getError())
                .contains("missing input");
    }

    @Test
    public void testVerify(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setSignature(signResult.getSignature()))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifyHmac(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var hmac = transitApi.hmac(keyName, null, data, null)
                .await().indefinitely();

        assertThat(hmac)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setHmac(hmac))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifyRSA(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setSignatureAlgorithm(PKCS1V15))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setSignature(signResult.getSignature())
                .setSignatureAlgorithm(PKCS1V15))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifyDerived(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ED25519)
                .setDerived(true))
                .await().indefinitely();

        var context = transitApi.random(16, null, null)
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setContext(context))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setSignature(signResult.getSignature())
                .setContext(context))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifySaltLength(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setSaltLength(23))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setSignature(signResult.getSignature())
                .setSaltLength(23))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifyMarshalingAlg(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data = "test".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signResult = transitApi.sign(keyName, new VaultSecretsTransitSignParams()
                .setInput(data)
                .setMarshalingAlgorithm(VaultSecretsTransitMarshalingAlgorithm.JWS))
                .await().indefinitely();

        assertThat(signResult)
                .isNotNull();

        var verifyResult = transitApi.verify(keyName, new VaultSecretsTransitVerifyParams()
                .setInput(data)
                .setSignature(signResult.getSignature())
                .setMarshalingAlgorithm(VaultSecretsTransitMarshalingAlgorithm.JWS))
                .await().indefinitely();

        assertThat(verifyResult)
                .isTrue();
    }

    @Test
    public void testVerifyBatch(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var signature = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signature)
                .hasSize(2);

        var verify = transitApi.verifyBatch(keyName, new VaultSecretsTransitVerifyBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data1)
                                .setSignature(signature.get(0).getSignature())
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data2)
                                .setSignature(signature.get(1).getSignature())
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(verify)
                .hasSize(2);

        var verify1 = verify.get(0);

        assertThat(verify1.isValid())
                .isTrue();
        assertThat(verify1.getError())
                .isNull();
        assertThat(verify1.getReference())
                .isEqualTo("item1");

        var verify2 = verify.get(1);

        assertThat(verify2.isValid())
                .isTrue();
        assertThat(verify2.getError())
                .isNull();
        assertThat(verify2.getReference())
                .isEqualTo("item2");
    }

    @Test
    public void testVerifyBatchHMAC(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(ECDSA_P256))
                .await().indefinitely();

        var hmac = transitApi.hmacBatch(keyName, new VaultSecretsTransitHmacBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitHmacBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(hmac)
                .hasSize(2);

        var verify = transitApi.verifyBatch(keyName, new VaultSecretsTransitVerifyBatchParams()
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data1)
                                .setHmac(hmac.get(0).getHmac())
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data2)
                                .setHmac(hmac.get(1).getHmac())
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(verify)
                .hasSize(2);
        assertThat(verify.get(0).isValid())
                .isTrue();
        assertThat(verify.get(1).isValid())
                .isTrue();
    }

    @Test
    public void testVerifyBatchRSA(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setSignatureAlgorithm(PKCS1V15)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signResult)
                .hasSize(2);

        var verify = transitApi.verifyBatch(keyName, new VaultSecretsTransitVerifyBatchParams()
                .setSignatureAlgorithm(PKCS1V15)
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data1)
                                .setSignature(signResult.get(0).getSignature())
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data2)
                                .setSignature(signResult.get(1).getSignature())
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(verify)
                .hasSize(2);
        assertThat(verify.get(0).isValid())
                .isTrue();
        assertThat(verify.get(1).isValid())
                .isTrue();
    }

    @Test
    public void testVerifyBatchSaltLength(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setSaltLength(23)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signResult)
                .hasSize(2);

        var verify = transitApi.verifyBatch(keyName, new VaultSecretsTransitVerifyBatchParams()
                .setSaltLength(23)
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data1)
                                .setSignature(signResult.get(0).getSignature())
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data2)
                                .setSignature(signResult.get(1).getSignature())
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(verify)
                .hasSize(2);
        assertThat(verify.get(0).isValid())
                .isTrue();
        assertThat(verify.get(1).isValid())
                .isTrue();
    }

    @Test
    public void testVerifyBatchMarshalingAlg(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var data1 = "hello".getBytes();
        var data2 = "transit".getBytes();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(RSA_2048))
                .await().indefinitely();

        var signResult = transitApi.signBatch(keyName, new VaultSecretsTransitSignBatchParams()
                .setMarshalingAlgorithm(VaultSecretsTransitMarshalingAlgorithm.JWS)
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data1)
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitSignBatchItem()
                                .setInput(data2)
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(signResult)
                .hasSize(2);

        var verify = transitApi.verifyBatch(keyName, new VaultSecretsTransitVerifyBatchParams()
                .setMarshalingAlgorithm(VaultSecretsTransitMarshalingAlgorithm.JWS)
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data1)
                                .setSignature(signResult.get(0).getSignature())
                                .setReference("item1"))
                .addBatchItem(
                        new VaultSecretsTransitVerifyBatchItem()
                                .setInput(data2)
                                .setSignature(signResult.get(1).getSignature())
                                .setReference("item2")))
                .await().indefinitely();

        assertThat(verify)
                .hasSize(2);
        assertThat(verify.get(0).isValid())
                .isTrue();
        assertThat(verify.get(1).isValid())
                .isTrue();
    }

    @Test
    public void testBackupResource(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setExportable(true)
                .setAllowPlaintextBackup(true))
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        var backup = transitApi.backupKey(keyName)
                .await().indefinitely();

        assertThat(backup)
                .isNotEmpty();

        assertThatCode(() -> transitApi.restoreKey(keyName, backup, null)
                .await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("already exists");

        transitApi.restoreKey(keyName, backup, true)
                .await().indefinitely();
    }

    @Test
    public void testTrimKey(VaultClient client, @Random String keyName) {
        var transitApi = client.secrets().transit();

        var key = transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(AES256_GCM96))
                .await().indefinitely();

        assertThat(key)
                .isNotNull();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();
        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        transitApi.updateKey(keyName, new VaultSecretsTransitUpdateKeyParams()
                .setMinDecryptionVersion(2)
                .setMinEncryptionVersion(2))
                .await().indefinitely();

        transitApi.trimKey(keyName, 2)
                .await().indefinitely();

        var trimmed = transitApi.readKey(keyName)
                .await().indefinitely();

        assertThat(trimmed)
                .isNotNull();
        assertThat(trimmed.getLatestVersion())
                .isEqualTo(3);
        assertThat(trimmed.getMinAvailableVersion())
                .isEqualTo(2);
        assertThat(trimmed.getMinDecryptionVersion())
                .isEqualTo(2);
        assertThat(trimmed.getMinEncryptionVersion())
                .isEqualTo(2);
    }

    @Test
    public void testUpdateCacheConfig(VaultClient client) {
        var transitApi = client.secrets().transit();

        transitApi.updateCacheConfig(123)
                .await().indefinitely();

        var size = transitApi.readCacheConfig()
                .await().indefinitely();

        assertThat(size)
                .isEqualTo(123);
    }

    @Test
    public void testAllKeysConfig(VaultClient client) {
        var transitApi = client.secrets().transit();

        transitApi.updateKeysConfig(new VaultSecretsTransitUpdateKeysConfigParams()
                .setDisableUpsert(true))
                .await().indefinitely();

        var config = transitApi.readKeysConfig()
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.isDisableUpsert())
                .isTrue();

        transitApi.updateKeysConfig(new VaultSecretsTransitUpdateKeysConfigParams()
                .setDisableUpsert(false))
                .await().indefinitely();

        config = transitApi.readKeysConfig()
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.isDisableUpsert())
                .isFalse();
    }

    @Test
    public void testSecureExportImport(VaultClient client, @Random String keyName, @Random String keyName2) {
        var transitApi = client.secrets().transit();

        var wrappingKey = transitApi.getWrappingKey()
                .await().indefinitely();
        transitApi.importKey("wrapping-key", new VaultSecretsTransitImportKeyParams()
                .setType(RSA_4096)
                .setPublicKey(wrappingKey))
                .await().indefinitely();

        var key = transitApi.createKey(keyName, new VaultSecretsTransitCreateKeyParams()
                .setType(AES256_GCM96)
                .setExportable(true))
                .await().indefinitely();

        transitApi.rotateKey(keyName, null)
                .await().indefinitely();

        assertThat(key)
                .isNotNull();

        var export = transitApi.secureExportKey("wrapping-key", keyName, null)
                .await().indefinitely();

        assertThat(export)
                .isNotNull();
        assertThat(export.getKeys())
                .hasSize(2);

        transitApi.importKey(keyName2, new VaultSecretsTransitImportKeyParams()
                .setCiphertext(export.getKeys().get("1")))
                .await().indefinitely();
        transitApi.importKeyVersion(keyName2, new VaultSecretsTransitImportKeyVersionParams()
                .setCiphertext(export.getKeys().get("2")))
                .await().indefinitely();

        var read = transitApi.readKey(keyName2)
                .await().indefinitely();

        assertThat(read)
                .isNotNull();
        assertThat(read.getLatestVersion())
                .isEqualTo(2);
    }

    private PublicKey exportPublicKey(VaultSecretsTransit transitApi, String keyName, int version, boolean isRSA)
            throws Exception {

        var key = transitApi.exportKey(PUBLIC_KEY, keyName, String.valueOf(version))
                .await().indefinitely()
                .getKeys().get(String.valueOf(version));

        var spki = (SubjectPublicKeyInfo) new PEMParser(new StringReader(key)).readObject();
        if (isRSA) {
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(spki.getEncoded()));
        } else {
            return KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(spki.getEncoded()));
        }
    }

}

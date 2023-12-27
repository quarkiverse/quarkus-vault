package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.secrets.totp.VaultSecretsTOTPCreateKeyParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(secrets = {
        @Mount(type = "totp", path = "totp")
})
public class VaultSecretsTOTPTest {

    private static final String TEST_OTP_URL = "otpauth://totp/Vault:test@google.com?secret=Y64VEVMBTSXCYIWRSHRNDZW62MPGVU2G&issuer=Vault";

    @Test
    public void testCreate(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        var totp = totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL))
                .await().indefinitely();

        assertThat(totp)
                .isEmpty();
    }

    @Test
    public void testCreateGenerated(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        var totp = totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(true)
                .setIssuer("Google")
                .setAccountName("test@gmail.com"))
                .await().indefinitely();

        assertThat(totp)
                .isNotEmpty();

        var totpVal = totp.get();

        assertThat(totpVal.getUrl())
                .isNotEmpty();
        assertThat(totpVal.getBarcode())
                .isNotEmpty();
    }

    @Test
    public void testRead(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL)
                .setAlgorithm("SHA256"))
                .await().indefinitely();

        var keyInfo = totpApi.readKey(key)
                .await().indefinitely();

        assertThat(keyInfo)
                .isNotNull();

        assertThat(keyInfo.getAccountName())
                .isEqualTo("test@google.com");
        assertThat(keyInfo.getIssuer())
                .isEqualTo("Vault");
        assertThat(keyInfo.getAlgorithm())
                .isEqualTo("SHA256");
        assertThat(keyInfo.getDigits())
                .isEqualTo(6);
        assertThat(keyInfo.getPeriod())
                .isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    public void testList(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL))
                .await().indefinitely();

        var keys = totpApi.listKeys()
                .await().indefinitely();

        assertThat(keys)
                .contains(key);
    }

    @Test
    public void testDelete(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL))
                .await().indefinitely();

        var keys = totpApi.listKeys()
                .await().indefinitely();

        assertThat(keys)
                .contains(key);

        totpApi.deleteKey(key)
                .await().indefinitely();

        keys = totpApi.listKeys()
                .await().indefinitely();

        assertThat(keys)
                .doesNotContain(key);
    }

    @Test
    public void testGenerateCode(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL))
                .await().indefinitely();

        var code = totpApi.generateCode(key)
                .await().indefinitely();

        assertThat(code)
                .isNotEmpty();
    }

    @Test
    public void testValidateCode(VaultClient client, @Random String key) {
        var totpApi = client.secrets().totp();

        totpApi.createKey(key, new VaultSecretsTOTPCreateKeyParams()
                .setGenerate(false)
                .setUrl(TEST_OTP_URL))
                .await().indefinitely();

        var code = totpApi.generateCode(key)
                .await().indefinitely();

        assertThat(code)
                .isNotEmpty();

        var valid = totpApi.validateCode(key, code)
                .await().indefinitely();

        assertThat(valid)
                .isTrue();
    }
}

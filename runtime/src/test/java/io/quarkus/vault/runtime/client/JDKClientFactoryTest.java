package io.quarkus.vault.runtime.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Verifies the fix for https://github.com/quarkiverse/quarkus-vault/issues/347
 *
 * A {@code quarkus.vault.tls.ca-cert} file that contains more than one certificate (a CA chain of a
 * root plus an intermediate) must build a valid TLS context instead of failing with
 * "Invalid PEM Certificate".
 */
class JDKClientFactoryTest {

    private static final String ROOT_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDDzCCAfegAwIBAgIUGpxMft32xgOn+WaUDPcjqn2M03UwDQYJKoZIhvcNAQEL\n" +
            "BQAwFzEVMBMGA1UEAwwMVGVzdCBSb290IENBMB4XDTI2MDcyMTEwNTQwNVoXDTM2\n" +
            "MDcxODEwNTQwNVowFzEVMBMGA1UEAwwMVGVzdCBSb290IENBMIIBIjANBgkqhkiG\n" +
            "9w0BAQEFAAOCAQ8AMIIBCgKCAQEA41bMINlOlcp4ikMRTbrD6Woz8oZAzLB+ntbb\n" +
            "up18LeWvzhKzSQvKHpYYG/16nu4bYEwJbwJlLffY+XbLSmuOMv2PCZ0i8r5Fjo0J\n" +
            "MSiLVPMiVeg5Wv09HpdWT/Ke8CiZm88xWxc01871sVkqj+jYb9VEQDU5oR61iG1Q\n" +
            "eV+raNrsjF6yxHPz8mKCRrEWjNmXg2S5gAjQKU8IiqesKrMSP0K7rbcFK40CpcIw\n" +
            "7rpuFg88voL++xgnpG3hkNjrrCMIDjAFAZ/VcWrOsttA5CZgCaEk18u9WmQF6CFF\n" +
            "KK385Jaji6WjcaZb+7kgMRM3TLR43vhLRk4IIIV6ACYJkKQbewIDAQABo1MwUTAd\n" +
            "BgNVHQ4EFgQUv4rTf8g9ZfoJYI6NdDo7OjhfZywwHwYDVR0jBBgwFoAUv4rTf8g9\n" +
            "ZfoJYI6NdDo7OjhfZywwDwYDVR0TAQH/BAUwAwEB/zANBgkqhkiG9w0BAQsFAAOC\n" +
            "AQEARfNTSAbly88RvuBF3LlCeghtO/BxR+pvTKlaMhzHJz8u82cIgQmQtIccb9iO\n" +
            "u0Hp94Yta3iUz4rds45x+KZVrIiLDmQbT3Ty8Av2Kit/3U5frTg9g9sK4lCrj17m\n" +
            "G5KTDKS9MfzAsMJFnXo89e1VSNvxZptukVq3/CvFQ+7si7tKAz6BoB5reHUQpmcw\n" +
            "WMdMzuMOR0Y3v5dAafFPGbC0sPJV7TAD7vA5bh1JRV6G7dPr4JvnmDZbGoR026Tg\n" +
            "HpiFMuX4uf4nFsgbcBnbxIKNGxFC19Mn6z7FyrmuopBsbhpOK0+1NCGrFeLIsufV\n" +
            "82GRrupEJgC7mwcFC9DHF4HJ7w==\n" +
            "-----END CERTIFICATE-----\n";

    private static final String INTERMEDIATE_CERT = "-----BEGIN CERTIFICATE-----\n" +
            "MIIDHzCCAgegAwIBAgIUZgSU0wSbLf3cc+bTIqANSWwIMfQwDQYJKoZIhvcNAQEL\n" +
            "BQAwHzEdMBsGA1UEAwwUVGVzdCBJbnRlcm1lZGlhdGUgQ0EwHhcNMjYwNzIxMTA1\n" +
            "NDA1WhcNMzYwNzE4MTA1NDA1WjAfMR0wGwYDVQQDDBRUZXN0IEludGVybWVkaWF0\n" +
            "ZSBDQTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAMK7SwYg8X4KiKVm\n" +
            "4Ls8Ch3hL62aPyi1/CYQ2okwWl2W9aYUeFQWNhS73kzViTjs8vDUbhyhzBgWht4E\n" +
            "sJ0AIbSW/pkRbL06BKio7a/1sYLRnF0l2xRxYY8OmUY8wKiGKkElL5VNupL9jTM+\n" +
            "MSR56q2AdD3suU+YHTGFbs3iN61+FYGDouguyuRpKaKXQjfACF07ukbyA0Dgm/wr\n" +
            "q39rdtWh7BMU2irDS3BHhwOihCKQcmjgtvwqzwo6S3iyX1m42L+Wh9f4GbKamcPk\n" +
            "biKIbUDIoYPsXgANeE7xLYDpTmHEn5t/Mlmu+S1KdfovnPjCHvyVaC2ehQK9b3dm\n" +
            "1By5aXECAwEAAaNTMFEwHQYDVR0OBBYEFPEcN0lvqC2TEClpWleCbBz5OWXmMB8G\n" +
            "A1UdIwQYMBaAFPEcN0lvqC2TEClpWleCbBz5OWXmMA8GA1UdEwEB/wQFMAMBAf8w\n" +
            "DQYJKoZIhvcNAQELBQADggEBAHdkLmfcQyIXpYAlWCSaw+xPJtpKcztpT2vqFETa\n" +
            "5gbyahjDZk6o6m0N9n8TPtR/fFcbvq1PXYV2T8KMK57+P3KLGTD69xz+dtddq1sy\n" +
            "Cecvu4LH7kCPOVFg7f9+OHSEfYly+1d30GEE14wdEa/GWI7jEmp0FHbhiaM12C46\n" +
            "cRI8BGsGKAOTbP2oFgfuJR/OYNZ+o6Y1Wme5Y2vivbOeWSJxwNGi2lp6aevf335u\n" +
            "fNSkq7b9N1Ji5aVG41n+gK8OwS+/c0QzS/72MC/g1CKxPoffA4JtJSqMbAQryQ66\n" +
            "aq7Itjel6w5wcxtffSC2tpl0J5FjclQa6zWmK2tEurhmPh4=\n" +
            "-----END CERTIFICATE-----\n";

    @Test
    void buildsSslContextFromMultiCertificatePem(@TempDir Path tempDir) throws IOException {
        Path caCert = tempDir.resolve("ca-chain.pem");
        Files.writeString(caCert, ROOT_CERT + INTERMEDIATE_CERT, UTF_8);

        SSLContext sslContext = JDKClientFactory.buildSslContextFromPem(caCert.toString());

        assertNotNull(sslContext);
    }
}

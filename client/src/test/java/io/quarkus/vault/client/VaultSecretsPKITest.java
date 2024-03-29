package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.bouncycastle.asn1.x500.style.BCStyle.CN;

import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.stream.Stream;

import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.cert.X509CRLEntryHolder;
import org.bouncycastle.cert.X509CRLHolder;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.secrets.pki.*;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest(secrets = {
        @VaultClientTest.Mount(path = "pki", type = "pki"),
})
public class VaultSecretsPKITest {

    @Test
    public void testIssue(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNotEmpty();
        assertThat(issued.getPrivateKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);
    }

    @Test
    public void testIssueViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue(root.getIssuerId(), "test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNotEmpty();
        assertThat(issued.getPrivateKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);
    }

    @Test
    public void testSign(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var csr = new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder()
                .addRDN(CN, "test.example.com").build(), keyPair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));
        var csrPem = new StringWriter();
        try (var pemWriter = new JcaPEMWriter(csrPem)) {
            pemWriter.writeObject(csr);
        }

        var issued = pki.sign("test", new VaultSecretsPKISignParams()
                .setCsr(csrPem.toString()))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNull();
        assertThat(issued.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var csr = new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder()
                .addRDN(CN, "test.example.com").build(), keyPair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));
        var csrPem = new StringWriter();
        try (var pemWriter = new JcaPEMWriter(csrPem)) {
            pemWriter.writeObject(csr);
        }

        var issued = pki.sign(root.getIssuerId(), "test", new VaultSecretsPKISignParams()
                .setCsr(csrPem.toString()))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNull();
        assertThat(issued.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignVerbatim(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var csr = new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder()
                .addRDN(CN, "test.example.com").build(), keyPair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));
        var csrPem = new StringWriter();
        try (var pemWriter = new JcaPEMWriter(csrPem)) {
            pemWriter.writeObject(csr);
        }

        var issued = pki.signVerbatim("test", new VaultSecretsPKISignVerbatimParams()
                .setCsr(csrPem.toString()))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNull();
        assertThat(issued.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignVerbatimViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        var csr = new JcaPKCS10CertificationRequestBuilder(new X500NameBuilder()
                .addRDN(CN, "test.example.com").build(), keyPair.getPublic())
                .build(new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate()));
        var csrPem = new StringWriter();
        try (var pemWriter = new JcaPEMWriter(csrPem)) {
            pemWriter.writeObject(csr);
        }

        var issued = pki.signVerbatim(root.getIssuerId(), "test", new VaultSecretsPKISignVerbatimParams()
                .setCsr(csrPem.toString()))
                .toCompletableFuture().get();

        assertThat(issued.getCertificate())
                .isNotEmpty();
        assertThat(issued.getSerialNumber())
                .isNotEmpty();
        assertThat(issued.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusDays(30).plusSeconds(5));
        assertThat(issued.getIssuingCa())
                .isNotEmpty();
        assertThat(issued.getCaChain())
                .isNotEmpty();
        assertThat(issued.getPrivateKey())
                .isNull();
        assertThat(issued.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignIntermediate(VaultClient client, @Random String mount1, @Random String mount2) throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);
        var pki2 = client.secrets().pki(mount2);

        pki1.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var genCsr = pki2.generateIntermediateCsr(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Test Intermediate CA"))
                .toCompletableFuture().get();

        var signed = pki1.signIntermediate(new VaultSecretsPKISignIntermediateParams()
                .setCsr(genCsr.getCsr())
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        assertThat(signed.getCertificate())
                .isNotEmpty();
        assertThat(signed.getSerialNumber())
                .isNotEmpty();
        assertThat(signed.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusMonths(30).plusSeconds(5));
        assertThat(signed.getCaChain())
                .hasSize(2);
        assertThat(signed.getIssuingCa())
                .isNotEmpty();
        assertThat(signed.getPrivateKey())
                .isNull();
        assertThat(signed.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignIntermediateViaIssuer(VaultClient client, @Random String mount1, @Random String mount2)
            throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);
        var pki2 = client.secrets().pki(mount2);

        var root = pki1.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var genCsr = pki2.generateIntermediateCsr(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Test Intermediate CA"))
                .toCompletableFuture().get();

        var signed = pki1.signIntermediate(root.getIssuerId(), new VaultSecretsPKISignIntermediateParams()
                .setCsr(genCsr.getCsr())
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        assertThat(signed.getCertificate())
                .isNotEmpty();
        assertThat(signed.getSerialNumber())
                .isNotEmpty();
        assertThat(signed.getExpiration())
                .isBetween(now().plusDays(30).minusSeconds(5), now().plusMonths(30).plusSeconds(5));
        assertThat(signed.getCaChain())
                .hasSize(2);
        assertThat(signed.getIssuingCa())
                .isNotEmpty();
        assertThat(signed.getPrivateKey())
                .isNull();
        assertThat(signed.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testSignSelfIssued(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var signed = pki.signSelfIssued(root.getCertificate(), false)
                .toCompletableFuture().get();

        assertThat(signed.getCertificate())
                .isNotEmpty();
        assertThat(signed.getIssuingCa())
                .isNotEmpty();
    }

    @Test
    public void testSignSelfIssuedViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var signed = pki.signSelfIssued(root.getIssuerId(), root.getCertificate(), false)
                .toCompletableFuture().get();

        assertThat(signed.getCertificate())
                .isNotEmpty();
        assertThat(signed.getIssuingCa())
                .isNotEmpty();
    }

    @Test
    public void testListCerts(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        var certs = pki.listCertificates()
                .toCompletableFuture().get();

        assertThat(certs)
                .contains(issued.getSerialNumber());
    }

    @Test
    public void testReadCert(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        var certInfo = pki.readCertificate(issued.getSerialNumber())
                .toCompletableFuture().get();

        assertThat(certInfo.getCertificate())
                .isEqualTo(issued.getCertificate());
        assertThat(certInfo.getCaChain())
                .isNull();
        assertThat(certInfo.getRevocationTime())
                .isNull();

    }

    @Test
    public void testRevoke(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        var revoked = pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(issued.getSerialNumber()))
                .toCompletableFuture().get();

        assertThat(revoked.getRevocationTime())
                .isBetween(now().minusSeconds(5), now().plusSeconds(5));
    }

    @Test
    public void testRevokeWithKey(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        var revoked = pki.revokeWithKey(new VaultSecretsPKIRevokeWithKeyParams()
                .setSerialNumber(issued.getSerialNumber())
                .setPrivateKey(issued.getPrivateKey()))
                .toCompletableFuture().get();

        assertThat(revoked.getRevocationTime())
                .isBetween(now().minusSeconds(5), now().plusSeconds(5));
    }

    @Test
    public void testListRevoked(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setTtl(Duration.ofDays(30)))
                .toCompletableFuture().get();

        var issued = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com"))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(issued.getSerialNumber()))
                .toCompletableFuture().get();

        var revoked = pki.listRevoked()
                .toCompletableFuture().get();

        assertThat(revoked)
                .contains(issued.getSerialNumber());
    }

    @Test
    public void testReadIssuerCa(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA 1"))
                .toCompletableFuture().get();

        var ca = pki.readIssuerCaCert()
                .toCompletableFuture().get();

        assertThat(ca.getCertificate().trim())
                .isEqualTo(root.getCertificate().trim());
    }

    @Test
    public void testReadIssuerCaFromIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA 1"))
                .toCompletableFuture().get();

        var ca = pki.readIssuerCaCert(root.getIssuerId())
                .toCompletableFuture().get();

        assertThat(ca.getCertificate().trim())
                .isEqualTo(root.getCertificate().trim());
        assertThat(ca.getCaChain())
                .hasSize(1);
        assertThat(ca.getRevocationTime())
                .isNull();
    }

    @Test
    public void testReadIssuerCaChain(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA 1"))
                .toCompletableFuture().get();

        var caChain = pki.readIssuerCaChain()
                .toCompletableFuture().get();

        assertThat(caChain.trim())
                .isEqualTo(root.getCertificate().trim());
    }

    @Test
    public void testReadIssuerCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerCrl()
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        var certSn = new BigInteger(HexFormat.ofDelimiter(":").parseHex(cert.getSerialNumber()));

        assertThat(crlSns)
                .contains(certSn);
    }

    @Test
    public void testReadIssuerCrlViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerCrl(root.getIssuerId())
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        var certSn = new BigInteger(HexFormat.ofDelimiter(":").parseHex(cert.getSerialNumber()));

        assertThat(crlSns)
                .contains(certSn);
    }

    @Test
    public void testReadIssuerDeltaCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerDeltaCrl()
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        assertThat(crlSns)
                .isEmpty();
    }

    @Test
    public void testReadIssuerDeltaCrlViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerDeltaCrl(root.getIssuerId())
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        assertThat(crlSns)
                .isEmpty();
    }

    @Disabled("Requires Enterprise Vault")
    @Test
    public void testReadIssuerUnifiedCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerUnifiedCrl()
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        var certSn = new BigInteger(HexFormat.ofDelimiter(":").parseHex(cert.getSerialNumber()));

        assertThat(crlSns)
                .contains(certSn);
    }

    @Disabled("Requires Enterprise Vault")
    @Test
    public void testReadIssuerUnifiedCrlViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerUnifiedCrl(root.getIssuerId())
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        var certSn = new BigInteger(HexFormat.ofDelimiter(":").parseHex(cert.getSerialNumber()));

        assertThat(crlSns)
                .contains(certSn);
    }

    @Disabled("Requires Enterprise Vault")
    @Test
    public void testReadIssuerUnifiedDeltaCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerUnifiedDeltaCrl()
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        assertThat(crlSns)
                .isEmpty();
    }

    @Disabled("Requires Enterprise Vault")
    @Test
    public void testReadIssuerUnifiedDeltaCrlViaIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA ")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setAllowAnyName(true)
                .setUseCsrCommonName(true))
                .toCompletableFuture().get();

        var cert = pki.issue("test", new VaultSecretsPKIIssueParams()
                .setCommonName("test.example.com")
                .setNotAfter(OffsetDateTime.now().plusMonths(3)))
                .toCompletableFuture().get();

        pki.revoke(new VaultSecretsPKIRevokeParams()
                .setSerialNumber(cert.getSerialNumber()))
                .toCompletableFuture().get();

        var crlStr = pki.readIssuerUnifiedDeltaCrl(root.getIssuerId())
                .toCompletableFuture().get();

        var crl = (X509CRLHolder) new PEMParser(new StringReader(crlStr)).readObject();
        @SuppressWarnings("unchecked")
        var crlSns = (Stream<BigInteger>) crl.getRevokedCertificates().stream()
                .map(c -> ((X509CRLEntryHolder) c).getSerialNumber());

        assertThat(crlSns)
                .isEmpty();
    }

    @Test
    public void testConfigUrls(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.configUrls(new VaultSecretsPKIConfigUrlsParams()
                .setEnableTemplating(true)
                .setOcspServers(List.of("https://example.com/ocsp"))
                .setCrlDistributionPoints(List.of("https://example.com/crl.pem")))
                .toCompletableFuture().get();

        var config = pki.readUrlsConfig().toCompletableFuture().get();

        assertThat(config.isEnableTemplating())
                .isTrue();
        assertThat(config.getCrlDistributionPoints())
                .contains("https://example.com/crl.pem");
        assertThat(config.getOcspServers())
                .contains("https://example.com/ocsp");
    }

    @Test
    public void testConfigCluster(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.configCluster("https://example.com/1", "https://example.com/2")
                .toCompletableFuture().get();

        var config = pki.readClusterConfig().toCompletableFuture().get();

        assertThat(config.getPath())
                .isEqualTo("https://example.com/1");
        assertThat(config.getAiaPath())
                .isEqualTo("https://example.com/2");
    }

    @Test
    public void testConfigCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.configCrl(new VaultSecretsPKIConfigCrlParams()
                .setDisable(false)
                .setAutoRebuild(true)
                .setAutoRebuildGracePeriod(Duration.ofHours(6))
                .setExpiry(Duration.ofDays(31))
                .setEnableDelta(true)
                .setDeltaRebuildInterval(Duration.ofMinutes(30))
                .setCrossClusterRevocation(false)
                .setOcspDisable(false)
                .setOcspExpiry(Duration.ofHours(7)))
                .toCompletableFuture().get();

        var config = pki.readCrlConfig().toCompletableFuture().get();

        assertThat(config.isDisable())
                .isFalse();
        assertThat(config.isAutoRebuild())
                .isTrue();
        assertThat(config.getAutoRebuildGracePeriod())
                .isEqualTo(Duration.ofHours(6));
        assertThat(config.getExpiry())
                .isEqualTo(Duration.ofDays(31));
        assertThat(config.isEnableDelta())
                .isTrue();
        assertThat(config.getDeltaRebuildInterval())
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(config.isCrossClusterRevocation())
                .isFalse();
        assertThat(config.isOcspDisable())
                .isFalse();
        assertThat(config.getOcspExpiry())
                .isEqualTo(Duration.ofHours(7));
    }

    @Test
    public void testConfigKeys(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var key1 = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, null)
                .toCompletableFuture().get();
        var key2 = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, null)
                .toCompletableFuture().get();

        pki.configKeys(key2.getKeyId())
                .toCompletableFuture().get();

        var keys = pki.readKeysConfig()
                .toCompletableFuture().get();

        assertThat(keys.getDefaultKey())
                .isEqualTo(key2.getKeyId());

        pki.configKeys(key1.getKeyId())
                .toCompletableFuture().get();

        keys = pki.readKeysConfig()
                .toCompletableFuture().get();

        assertThat(keys.getDefaultKey())
                .isEqualTo(key1.getKeyId());
    }

    @Test
    public void testConfigIssuers(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var issuer1 = pki.generateIssuerRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA"))
                .toCompletableFuture().get();
        var issuer2 = pki.generateIssuerRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA 2"))
                .toCompletableFuture().get();

        pki.configIssuers(issuer2.getIssuerId(), false)
                .toCompletableFuture().get();

        var issuers = pki.readIssuersConfig()
                .toCompletableFuture().get();

        assertThat(issuers.getDefaultIssuer())
                .isEqualTo(issuer2.getIssuerId());
        assertThat(issuers.isDefaultFollowsLatestIssuer())
                .isFalse();

        pki.configIssuers(issuer1.getIssuerId(), true)
                .toCompletableFuture().get();

        issuers = pki.readIssuersConfig()
                .toCompletableFuture().get();

        assertThat(issuers.getDefaultIssuer())
                .isEqualTo(issuer1.getIssuerId());
        assertThat(issuers.isDefaultFollowsLatestIssuer())
                .isTrue();
    }

    @Test
    public void testRotateCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var rotated = pki.rotateCrl()
                .toCompletableFuture().get();

        assertThat(rotated.isSuccess())
                .isTrue();
    }

    @Test
    public void testRotateDeltaCrl(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.configCrl(new VaultSecretsPKIConfigCrlParams()
                .setAutoRebuild(true)
                .setEnableDelta(true))
                .toCompletableFuture().get();

        var rotated = pki.rotateDeltaCrl()
                .toCompletableFuture().get();

        assertThat(rotated.isSuccess())
                .isTrue();
    }

    @Test
    public void testGenerateRoot(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        var cert = (X509CertificateHolder) new PEMParser(new StringReader(root.getCertificate())).readObject();

        assertThat(cert.getSubject().toString())
                .isEqualTo("CN=Test Root CA");
        assertThat(root.getSerialNumber())
                .isNotEmpty();
        assertThat(root.getExpiration())
                .isBetween(now().plusYears(1).minusSeconds(5), now().plusYears(1).plusSeconds(5));
        assertThat(root.getIssuerId())
                .isNotEmpty();
        assertThat(root.getIssuerName())
                .isEmpty();
        assertThat(root.getIssuingCa())
                .isEqualTo(root.getCertificate());
        assertThat(root.getKeyId())
                .isNotEmpty();
        assertThat(root.getKeyName())
                .isEmpty();
        assertThat(root.getPrivateKey())
                .isNotEmpty();
        assertThat(root.getPrivateKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);
    }

    @Test
    public void testRotateRoot(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA")
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        var rotated = pki.rotateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Rotated Root CA")
                .setNotAfter(OffsetDateTime.now().plusYears(2)))
                .toCompletableFuture().get();

        assertThat(rotated.getSerialNumber())
                .isNotEqualTo(root.getSerialNumber());

        var cert = (X509CertificateHolder) new PEMParser(new StringReader(rotated.getCertificate())).readObject();

        assertThat(cert.getSubject().toString())
                .isEqualTo("CN=Rotated Root CA");
        assertThat(rotated.getSerialNumber())
                .isNotEmpty();
        assertThat(rotated.getExpiration())
                .isBetween(now().plusYears(2).minusSeconds(5), now().plusYears(2).plusSeconds(5));
        assertThat(rotated.getIssuerId())
                .isNotEmpty();
        assertThat(rotated.getIssuerName())
                .isEqualTo("next");
        assertThat(rotated.getIssuingCa())
                .isEqualTo(rotated.getCertificate());
        assertThat(rotated.getKeyId())
                .isNotEmpty();
        assertThat(rotated.getKeyName())
                .isEmpty();
        assertThat(rotated.getPrivateKey())
                .isNull();
        assertThat(rotated.getPrivateKeyType())
                .isNull();
    }

    @Test
    public void testGenerateIssuerRoot(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateIssuerRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA")
                .setKeyType(VaultSecretsPKIKeyType.EC)
                .setKeyBits(VaultSecretsPKIKeyBits.EC_P384)
                .setNotAfter(OffsetDateTime.now().plusYears(1)))
                .toCompletableFuture().get();

        var cert = (X509CertificateHolder) new PEMParser(new StringReader(root.getCertificate())).readObject();

        assertThat(cert.getSubject())
                .asString()
                .isEqualTo("CN=Test Root CA");
        assertThat(root.getSerialNumber())
                .isNotEmpty();
        assertThat(root.getExpiration())
                .isBetween(now().plusYears(1).minusSeconds(5), now().plusYears(1).plusSeconds(5));
        assertThat(root.getIssuerId())
                .isNotEmpty();
        assertThat(root.getIssuerName())
                .isEmpty();
        assertThat(root.getIssuingCa())
                .isEqualTo(root.getCertificate());
        assertThat(root.getKeyId())
                .isNotEmpty();
        assertThat(root.getKeyName())
                .isEmpty();
        assertThat(root.getPrivateKey())
                .isNotEmpty();
        assertThat(root.getPrivateKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.EC);
    }

    @Test
    public void testGenerateIntermediateCsr(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var gen = pki.generateIntermediateCsr(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Test Intermediate CA"))
                .toCompletableFuture().get();

        var csr = (PKCS10CertificationRequest) new PEMParser(new StringReader(gen.getCsr())).readObject();

        assertThat(csr.getSubject())
                .asString()
                .isEqualTo("CN=Test Intermediate CA");
    }

    @Test
    public void testGenerateIssuerIntermediateCsr(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var gen = pki.generateIssuerIntermediateCsr(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Test Intermediate CA"))
                .toCompletableFuture().get();

        var csr = (PKCS10CertificationRequest) new PEMParser(new StringReader(gen.getCsr())).readObject();

        assertThat(csr.getSubject())
                .asString()
                .isEqualTo("CN=Test Intermediate CA");
    }

    @Test
    public void testGenerateCrossSignCsr(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var gen = pki.generateCrossSignCsr(new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Cross Intermediate CA"))
                .toCompletableFuture().get();

        var csr = (PKCS10CertificationRequest) new PEMParser(new StringReader(gen.getCsr())).readObject();

        assertThat(csr.getSubject())
                .asString()
                .isEqualTo("CN=Cross Intermediate CA");
    }

    @Test
    public void testSetSignedIntermediate(VaultClient client, @Random String mount1, @Random String mount2) throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);
        var pki2 = client.secrets().pki(mount2);

        pki1.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var genCsr = pki2.generateIntermediateCsr(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateCsrParams()
                .setCommonName("Test Intermediate CA"))
                .toCompletableFuture().get();

        var signed = pki1.signIntermediate(new VaultSecretsPKISignIntermediateParams()
                .setCsr(genCsr.getCsr()))
                .toCompletableFuture().get();

        var signedInt = pki2.setSignedIntermediate(signed.getCertificate())
                .toCompletableFuture().get();

        assertThat(signedInt.getImportedIssuers())
                .hasSize(1);
        assertThat(signedInt.getMapping())
                .containsKey(signedInt.getImportedIssuers().get(0));
        assertThat(signedInt.getExistingIssuers())
                .isNull();
        assertThat(signedInt.getExistingKeys())
                .isNull();
    }

    @Test
    public void testConfigCa(VaultClient client, @Random String mount1, @Random String mount2) throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);

        var root1 = pki1.generateRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var pemBundle = root1.getCertificate() + "\n" +
                root1.getPrivateKey() + "\n";

        var pki2 = client.secrets().pki(mount2);

        var root2 = pki2.configCa(pemBundle)
                .toCompletableFuture().get();

        assertThat(root2.getImportedIssuers())
                .hasSize(1);
        assertThat(root2.getMapping())
                .containsKey(root2.getImportedIssuers().get(0));
        assertThat(root2.getExistingIssuers())
                .isNull();
        assertThat(root2.getExistingKeys())
                .isNull();
    }

    @Test
    public void testImportIssuerBundle(VaultClient client, @Random String mount1, @Random String mount2) throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);

        var root1 = pki1.generateRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var pemBundle = root1.getCertificate() + "\n" +
                root1.getPrivateKey() + "\n";

        var pki2 = client.secrets().pki(mount2);

        var root2 = pki2.importIssuerBundle(pemBundle)
                .toCompletableFuture().get();

        assertThat(root2.getImportedIssuers())
                .hasSize(1);
        assertThat(root2.getMapping())
                .containsKey(root2.getImportedIssuers().get(0));
        assertThat(root2.getExistingIssuers())
                .isNull();
        assertThat(root2.getExistingKeys())
                .isNull();
    }

    @Test
    public void testImportIssuerCert(VaultClient client, @Random String mount1, @Random String mount2) throws Exception {
        client.sys().mounts().enable(mount1, "pki", null, null, null)
                .toCompletableFuture().get();
        client.sys().mounts().enable(mount2, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki1 = client.secrets().pki(mount1);

        var root1 = pki1.generateRoot(VaultSecretsPKIManageType.EXPORTED, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var pki2 = client.secrets().pki(mount2);

        var root2 = pki2.importIssuerCertificate(root1.getCertificate())
                .toCompletableFuture().get();

        assertThat(root2.getImportedIssuers())
                .hasSize(1);
        assertThat(root2.getMapping())
                .containsKey(root2.getImportedIssuers().get(0));
        assertThat(root2.getExistingIssuers())
                .isNull();
        assertThat(root2.getExistingKeys())
                .isNull();
    }

    @Test
    public void testRevokeIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var revoked = pki.revokeIssuer(root.getIssuerId())
                .toCompletableFuture().get();

        assertThat(revoked.getRevocationTime())
                .isBetween(now().minusSeconds(5), now().plusSeconds(5));
        assertThat(revoked.getCertificate())
                .isNotEmpty();
        assertThat(revoked.getCaChain())
                .isNotEmpty();
        assertThat(revoked.getIssuerId())
                .isNotEmpty();
        assertThat(revoked.getIssuerName())
                .isEmpty();
        assertThat(revoked.getKeyId())
                .isNotEmpty();
        assertThat(revoked.getLeafNotAfterBehavior())
                .isEqualTo("err");
        assertThat(revoked.getManualChain())
                .isNull();
        assertThat(revoked.getUsage())
                .isNotEmpty();
    }

    @Test
    public void testDeleteIssuer(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var root = pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        assertThatCode(() -> pki.deleteIssuer(root.getIssuerId())
                .toCompletableFuture().get())
                .doesNotThrowAnyException();
    }

    @Test
    public void testListKeys(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var key1 = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, null)
                .toCompletableFuture().get();

        var key2 = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, null)
                .toCompletableFuture().get();

        var keys = pki.listKeys()
                .toCompletableFuture().get();

        assertThat(keys.getKeys())
                .contains(key1.getKeyId(), key2.getKeyId());
        assertThat(keys.getKeyInfo())
                .containsKeys(key1.getKeyId(), key2.getKeyId());
    }

    @Test
    public void testImportKey(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var privateKey = KeyPairGenerator.getInstance("RSA").generateKeyPair().getPrivate();
        var privateKeyPem = new StringWriter();
        try (var pemWriter = new JcaPEMWriter(privateKeyPem)) {
            pemWriter.writeObject(privateKey);
        }

        var key = pki.importKey(privateKeyPem.getBuffer().toString(), "Imported-Key-" + mount)
                .toCompletableFuture().get();

        assertThat(key.getKeyId())
                .isNotEmpty();
        assertThat(key.getKeyName())
                .isEqualTo("Imported-Key-" + mount);
        assertThat(key.getKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);

    }

    @Test
    public void testReadKey(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var key = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateKeyParams()
                .setKeyName("Test-Key"))
                .toCompletableFuture().get();

        var read = pki.readKey(key.getKeyId())
                .toCompletableFuture().get();

        assertThat(read.getKeyId())
                .isNotEmpty();
        assertThat(read.getKeyName())
                .isEqualTo("Test-Key");
        assertThat(read.getKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);
    }

    @Test
    public void testUpdateKey(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var key = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateKeyParams()
                .setKeyName("Test-Key"))
                .toCompletableFuture().get();

        var updated = pki.updateKey(key.getKeyId(), new VaultSecretsPKIUpdateKeyParams()
                .setKeyName("Test-Key-Updated"))
                .toCompletableFuture().get();

        assertThat(updated.getKeyId())
                .isNotEmpty();
        assertThat(updated.getKeyName())
                .isEqualTo("Test-Key-Updated");
        assertThat(updated.getKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.RSA);
    }

    @Test
    public void testDeleteKey(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var key = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateKeyParams()
                .setKeyName("Test-Key"))
                .toCompletableFuture().get();

        assertThatCode(() -> pki.deleteKey(key.getKeyId())
                .toCompletableFuture().get())
                .doesNotThrowAnyException();

        var keys = pki.listKeys()
                .toCompletableFuture().get();

        assertThat(keys.getKeys())
                .doesNotContain(key.getKeyId());
    }

    @Test
    public void testUpdateRole(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateIssuerRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setIssuerName("test1")
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        var now = OffsetDateTime.now();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams()
                .setIssuerRef("test1")
                .setTtl(Duration.ofMinutes(23))
                .setMaxTtl(Duration.ofMinutes(45))
                .setAllowLocalhost(true)
                .setAllowedDomains(List.of("example.com"))
                .setAllowedDomainsTemplate(true)
                .setAllowBareDomains(true)
                .setAllowSubdomains(true)
                .setAllowGlobDomains(true)
                .setAllowWildcardCertificates(true)
                .setAllowAnyName(true)
                .setEnforceHostnames(true)
                .setAllowIpSans(true)
                .setAllowedUriSans(List.of("https://example.com"))
                .setAllowedUriSansTemplate(true)
                .setAllowedOtherSans(List.of("2.5.29.17;UTF8:test@example.com"))
                .setAllowedSerialNumbers(List.of("1234567890"))
                .setServerFlag(true)
                .setClientFlag(true)
                .setCodeSigningFlag(true)
                .setEmailProtectionFlag(true)
                .setKeyType(VaultSecretsPKIKeyType.EC)
                .setKeyBits(VaultSecretsPKIKeyBits.EC_P256)
                .setSignatureBits(VaultSecretsPKISignatureBits.SHA_512)
                .setKeyUsage(List.of(VaultSecretsPKIKeyUsage.DIGITALSIGNATURE))
                .setExtKeyUsage(List.of(VaultSecretsPKIExtKeyUsage.CLIENTAUTH))
                .setExtKeyUsageOids(List.of("2.5.29.37"))
                .setUseCsrCommonName(true)
                .setUseCsrSans(true)
                .setOu(List.of("Test"))
                .setOrganization(List.of("Example Inc"))
                .setCountry(List.of("US"))
                .setProvince(List.of("CA"))
                .setLocality(List.of("San Francisco"))
                .setStreetAddress(List.of("123 Main Street"))
                .setPostalCode(List.of("94105"))
                .setGenerateLease(false)
                .setNoStore(true)
                .setRequireCn(true)
                .setPolicyIdentifiers(List.of())
                .setBasicConstraintsValidForNonCa(true)
                .setNotBefore(Duration.ofMinutes(5))
                .setNotAfter(now.plusYears(1))
                .setCnValidations(List.of(VaultSecretsPKICommonNameValidation.HOSTNAME))
                .setAllowedUserIds(List.of("user1")))
                .toCompletableFuture().get();

        var role = pki.readRole("test")
                .toCompletableFuture().get();

        assertThat(role.getIssuerRef())
                .isEqualTo("test1");
        assertThat(role.getTtl())
                .isEqualTo(Duration.ofMinutes(23));
        assertThat(role.getMaxTtl())
                .isEqualTo(Duration.ofMinutes(45));
        assertThat(role.isAllowLocalhost())
                .isTrue();
        assertThat(role.getAllowedDomains())
                .contains("example.com");
        assertThat(role.isAllowedDomainsTemplate())
                .isTrue();
        assertThat(role.isAllowBareDomains())
                .isTrue();
        assertThat(role.isAllowSubdomains())
                .isTrue();
        assertThat(role.isAllowGlobDomains())
                .isTrue();
        assertThat(role.isAllowWildcardCertificates())
                .isTrue();
        assertThat(role.isAllowAnyName())
                .isTrue();
        assertThat(role.isEnforceHostnames())
                .isTrue();
        assertThat(role.isAllowIpSans())
                .isTrue();
        assertThat(role.getAllowedUriSans())
                .contains("https://example.com");
        assertThat(role.isAllowedUriSansTemplate())
                .isTrue();
        assertThat(role.getAllowedOtherSans())
                .contains("2.5.29.17;UTF8:test@example.com");
        assertThat(role.getAllowedSerialNumbers())
                .contains("1234567890");
        assertThat(role.isServerFlag())
                .isTrue();
        assertThat(role.isClientFlag())
                .isTrue();
        assertThat(role.isCodeSigningFlag())
                .isTrue();
        assertThat(role.isEmailProtectionFlag())
                .isTrue();
        assertThat(role.getKeyType())
                .isEqualTo(VaultSecretsPKIKeyType.EC);
        assertThat(role.getKeyBits())
                .isEqualTo(VaultSecretsPKIKeyBits.EC_P256);
        assertThat(role.getSignatureBits())
                .isEqualTo(VaultSecretsPKISignatureBits.SHA_512);
        assertThat(role.getKeyUsage())
                .contains(VaultSecretsPKIKeyUsage.DIGITALSIGNATURE);
        assertThat(role.getExtKeyUsage())
                .contains(VaultSecretsPKIExtKeyUsage.CLIENTAUTH);
        assertThat(role.getExtKeyUsageOids())
                .contains("2.5.29.37");
        assertThat(role.isUseCsrCommonName())
                .isTrue();
        assertThat(role.isUseCsrSans())
                .isTrue();
        assertThat(role.getOu())
                .contains("Test");
        assertThat(role.getOrganization())
                .contains("Example Inc");
        assertThat(role.getCountry())
                .contains("US");
        assertThat(role.getProvince())
                .contains("CA");
        assertThat(role.getLocality())
                .contains("San Francisco");
        assertThat(role.getStreetAddress())
                .contains("123 Main Street");
        assertThat(role.getPostalCode())
                .contains("94105");
        assertThat(role.isGenerateLease())
                .isFalse();
        assertThat(role.isNoStore())
                .isTrue();
        assertThat(role.isRequireCn())
                .isTrue();
        assertThat(role.getPolicyIdentifiers())
                .isEmpty();
        assertThat(role.isBasicConstraintsValidForNonCa())
                .isTrue();
        assertThat(role.getNotBefore())
                .isEqualTo(Duration.ofMinutes(5));
        assertThat(role.getNotAfter())
                .isEqualTo(now.plusYears(1));
        assertThat(role.getCnValidations())
                .contains(VaultSecretsPKICommonNameValidation.HOSTNAME);
        assertThat(role.getAllowedUserIds())
                .contains("user1");
    }

    @Test
    public void testListRoles(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams())
                .toCompletableFuture().get();

        var roles = pki.listRoles()
                .toCompletableFuture().get();

        assertThat(roles)
                .contains("test");
    }

    @Test
    public void testDeleteRole(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        pki.generateRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Root CA"))
                .toCompletableFuture().get();

        pki.updateRole("test", new VaultSecretsPKIUpdateRoleParams())
                .toCompletableFuture().get();

        var roles = pki.listRoles()
                .toCompletableFuture().get();

        assertThat(roles)
                .contains("test");

        pki.deleteRole("test")
                .toCompletableFuture().get();

        roles = pki.listRoles()
                .toCompletableFuture().get();

        assertThat(roles)
                .doesNotContain("test");
    }

    @Test
    public void testDeleteAll(VaultClient client, @Random String mount) throws Exception {
        client.sys().mounts().enable(mount, "pki", null, null, null)
                .toCompletableFuture().get();

        var pki = client.secrets().pki(mount);

        var issuer = pki.generateIssuerRoot(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateRootParams()
                .setCommonName("Test Root CA"))
                .toCompletableFuture().get();

        var key = pki.generateKey(VaultSecretsPKIManageType.INTERNAL, new VaultSecretsPKIGenerateKeyParams()
                .setKeyName("Test-Key"))
                .toCompletableFuture().get();

        assertThatCode(() -> pki.deleteAll()
                .toCompletableFuture().get())
                .doesNotThrowAnyException();

        var keys = pki.listKeys()
                .toCompletableFuture().get();

        assertThat(keys.getKeys())
                .doesNotContain(key.getKeyId());

        var issuers = pki.listIssuers()
                .toCompletableFuture().get();

        assertThat(issuers.getKeys())
                .doesNotContain(issuer.getIssuerId());
    }
}

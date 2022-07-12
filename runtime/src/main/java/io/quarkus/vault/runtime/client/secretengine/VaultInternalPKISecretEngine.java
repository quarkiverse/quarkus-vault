package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICRLRotateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKICertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigCRLResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIConfigURLsResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateIntermediateCSRResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIGenerateRootResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRevokeCertificateResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleOptionsData;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRoleReadResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKIRolesListResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISetSignedIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestBody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignCertificateRequestResult;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKISignIntermediateCABody;
import io.quarkus.vault.runtime.client.dto.pki.VaultPKITidyBody;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.buffer.Buffer;

@Singleton
public class VaultInternalPKISecretEngine extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [PKI]";
    }

    private String getPath(String mount, String path) {
        return mount + "/" + path;
    }

    public Uni<Buffer> getCertificateAuthority(VaultClient vaultClient, String token, String mount, String format) {
        return getRaw(vaultClient, opName("Get CA"), token, mount, "ca", format);
    }

    public Uni<Buffer> getCertificateRevocationList(VaultClient vaultClient, String token, String mount, String format) {
        return getRaw(vaultClient, opName("Get CRL"), token, mount, "crl", format);
    }

    public Uni<Buffer> getCertificateAuthorityChain(VaultClient vaultClient, String token, String mount) {
        return getRaw(vaultClient, opName("Get CA Chain"), token, mount, "ca_chain", null);
    }

    private Uni<Buffer> getRaw(VaultClient vaultClient, String operationName, String token, String mount, String path,
            String format) {
        String suffix = format != null ? "/" + format : "";
        return vaultClient.get(operationName, getPath(mount, path + suffix), token)
                .replaceIfNullWith(Buffer.buffer());
    }

    public Uni<VaultPKICertificateResult> getCertificate(VaultClient vaultClient, String token, String mount, String serial) {
        return vaultClient.get(opName("Get Certificate"), getPath(mount, "cert/" + serial), token,
                VaultPKICertificateResult.class);
    }

    public Uni<VaultPKICertificateListResult> listCertificates(VaultClient vaultClient, String token, String mount) {
        return vaultClient.list(opName("List Certificates"), getPath(mount, "certs"), token,
                VaultPKICertificateListResult.class);
    }

    public Uni<Void> configCertificateAuthority(VaultClient vaultClient, String token, String mount,
            VaultPKIConfigCABody body) {
        return vaultClient.post(opName("Configure CA"), getPath(mount, "config/ca"), token, body, 204);
    }

    public Uni<VaultPKICRLRotateResult> rotateCertificateRevocationList(VaultClient vaultClient, String token, String mount) {
        return vaultClient.get(opName("Rotate CRL"), getPath(mount, "crl/rotate"), token, VaultPKICRLRotateResult.class);
    }

    public Uni<VaultPKIGenerateCertificateResult> generateCertificate(
            VaultClient vaultClient,
            String token,
            String mount,
            String role,
            VaultPKIGenerateCertificateBody body) {
        return vaultClient.post(opName("Issue Certificate"), getPath(mount, "issue/" + role), token, body,
                VaultPKIGenerateCertificateResult.class);
    }

    public Uni<VaultPKISignCertificateRequestResult> signCertificate(
            VaultClient vaultClient,
            String token,
            String mount,
            String role,
            VaultPKISignCertificateRequestBody body) {
        return vaultClient.post(opName("Sign Certificate"), getPath(mount, "sign/" + role), token, body,
                VaultPKISignCertificateRequestResult.class);
    }

    public Uni<VaultPKIRevokeCertificateResult> revokeCertificate(VaultClient vaultClient, String token, String mount,
            VaultPKIRevokeCertificateBody body) {
        return vaultClient.post(opName("Revoke Certificate"), getPath(mount, "revoke"), token, body,
                VaultPKIRevokeCertificateResult.class);
    }

    public Uni<Void> updateRole(VaultClient vaultClient, String token, String mount, String role,
            VaultPKIRoleOptionsData body) {
        return vaultClient.post(opName("Update Role"), getPath(mount, "roles/" + role), token, body, 204);
    }

    public Uni<VaultPKIRoleReadResult> readRole(VaultClient vaultClient, String token, String mount, String role) {
        return vaultClient.get(opName("Read Role"), getPath(mount, "roles/" + role), token, VaultPKIRoleReadResult.class);
    }

    public Uni<VaultPKIRolesListResult> listRoles(VaultClient vaultClient, String token, String mount) {
        return vaultClient.list(opName("List Roles"), getPath(mount, "roles"), token, VaultPKIRolesListResult.class);
    }

    public Uni<Void> deleteRole(VaultClient vaultClient, String token, String mount, String role) {
        return vaultClient.delete(opName("Delete Role"), getPath(mount, "roles/" + role), token, 204);
    }

    public Uni<VaultPKIGenerateRootResult> generateRoot(VaultClient vaultClient, String token, String mount, String type,
            VaultPKIGenerateRootBody body) {
        return vaultClient.post(opName("Generate Root CA"), getPath(mount, "root/generate/" + type), token, body,
                VaultPKIGenerateRootResult.class);
    }

    public Uni<Void> deleteRoot(VaultClient vaultClient, String token, String mount) {
        return vaultClient.delete(opName("Delete Root CA"), getPath(mount, "root"), token, 204);
    }

    public Uni<VaultPKISignCertificateRequestResult> signIntermediateCA(VaultClient vaultClient, String token, String mount,
            VaultPKISignIntermediateCABody body) {
        return vaultClient.post(opName("Sign Intermediate CA"), getPath(mount, "root/sign-intermediate"),
                token,
                body,
                VaultPKISignCertificateRequestResult.class);
    }

    public Uni<VaultPKIGenerateIntermediateCSRResult> generateIntermediateCSR(VaultClient vaultClient, String token,
            String mount, String type,
            VaultPKIGenerateIntermediateCSRBody body) {
        return vaultClient.post(opName("Generate Intermediate CSR"), getPath(mount, "intermediate/generate/" + type),
                token,
                body,
                VaultPKIGenerateIntermediateCSRResult.class);
    }

    public Uni<Void> setSignedIntermediateCA(VaultClient vaultClient, String token, String mount,
            VaultPKISetSignedIntermediateCABody body) {
        return vaultClient.post(opName("Update Signed Intermediate CA"), getPath(mount, "intermediate/set-signed"), token, body,
                204);
    }

    public Uni<Void> tidy(VaultClient vaultClient, String token, String mount, VaultPKITidyBody body) {
        return vaultClient.post(opName("Tidy"), getPath(mount, "tidy"), token, body, 202);
    }

    public Uni<Void> configURLs(VaultClient vaultClient, String token, String mount, VaultPKIConfigURLsData body) {
        return vaultClient.post(opName("Configure URLs"), getPath(mount, "config/urls"), token, body, 204);
    }

    public Uni<VaultPKIConfigURLsResult> readURLs(VaultClient vaultClient, String token, String mount) {
        return vaultClient.get(opName("Read URLs"), getPath(mount, "config/urls"), token, VaultPKIConfigURLsResult.class);
    }

    public Uni<Void> configCRL(VaultClient vaultClient, String token, String mount, VaultPKIConfigCRLData body) {
        return vaultClient.post(opName("Configure CRL"), getPath(mount, "config/crl"), token, body, 204);
    }

    public Uni<VaultPKIConfigCRLResult> readCRL(VaultClient vaultClient, String token, String mount) {
        return vaultClient.get(opName("Read CRL"), getPath(mount, "config/crl"), token, VaultPKIConfigCRLResult.class);
    }
}

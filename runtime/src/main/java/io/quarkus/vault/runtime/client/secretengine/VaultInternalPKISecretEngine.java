package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

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

    private String getPath(String mount, String path) {
        return mount + "/" + path;
    }

    public Uni<Buffer> getCertificateAuthority(String token, String mount, String format) {
        return getRaw(token, mount, "ca", format);
    }

    public Uni<Buffer> getCertificateRevocationList(String token, String mount, String format) {
        return getRaw(token, mount, "crl", format);
    }

    public Uni<Buffer> getCertificateAuthorityChain(String token, String mount) {
        return getRaw(token, mount, "ca_chain", null);
    }

    private Uni<Buffer> getRaw(String token, String mount, String path, String format) {
        String suffix = format != null ? "/" + format : "";
        return vaultClient.get(getPath(mount, path + suffix), token)
                .replaceIfNullWith(Buffer.buffer());
    }

    public Uni<VaultPKICertificateResult> getCertificate(String token, String mount, String serial) {
        return vaultClient.get(getPath(mount, "cert/" + serial), token, VaultPKICertificateResult.class);
    }

    public Uni<VaultPKICertificateListResult> listCertificates(String token, String mount) {
        return vaultClient.list(getPath(mount, "certs"), token, VaultPKICertificateListResult.class);
    }

    public Uni<Void> configCertificateAuthority(String token, String mount, VaultPKIConfigCABody body) {
        return vaultClient.post(getPath(mount, "config/ca"), token, body, 204);
    }

    public Uni<VaultPKICRLRotateResult> rotateCertificateRevocationList(String token, String mount) {
        return vaultClient.get(getPath(mount, "crl/rotate"), token, VaultPKICRLRotateResult.class);
    }

    public Uni<VaultPKIGenerateCertificateResult> generateCertificate(
            String token,
            String mount,
            String role,
            VaultPKIGenerateCertificateBody body) {
        return vaultClient.post(getPath(mount, "issue/" + role), token, body, VaultPKIGenerateCertificateResult.class);
    }

    public Uni<VaultPKISignCertificateRequestResult> signCertificate(
            String token,
            String mount,
            String role,
            VaultPKISignCertificateRequestBody body) {
        return vaultClient.post(getPath(mount, "sign/" + role), token, body, VaultPKISignCertificateRequestResult.class);
    }

    public Uni<VaultPKIRevokeCertificateResult> revokeCertificate(String token, String mount,
            VaultPKIRevokeCertificateBody body) {
        return vaultClient.post(getPath(mount, "revoke"), token, body, VaultPKIRevokeCertificateResult.class);
    }

    public Uni<Void> updateRole(String token, String mount, String role, VaultPKIRoleOptionsData body) {
        return vaultClient.post(getPath(mount, "roles/" + role), token, body, 204);
    }

    public Uni<VaultPKIRoleReadResult> readRole(String token, String mount, String role) {
        return vaultClient.get(getPath(mount, "roles/" + role), token, VaultPKIRoleReadResult.class);
    }

    public Uni<VaultPKIRolesListResult> listRoles(String token, String mount) {
        return vaultClient.list(getPath(mount, "roles"), token, VaultPKIRolesListResult.class);
    }

    public Uni<Void> deleteRole(String token, String mount, String role) {
        return vaultClient.delete(getPath(mount, "roles/" + role), token, 204);
    }

    public Uni<VaultPKIGenerateRootResult> generateRoot(String token, String mount, String type,
            VaultPKIGenerateRootBody body) {
        return vaultClient.post(getPath(mount, "root/generate/" + type), token, body, VaultPKIGenerateRootResult.class);
    }

    public Uni<Void> deleteRoot(String token, String mount) {
        return vaultClient.delete(getPath(mount, "root"), token, 204);
    }

    public Uni<VaultPKISignCertificateRequestResult> signIntermediateCA(String token, String mount,
            VaultPKISignIntermediateCABody body) {
        return vaultClient.post(getPath(mount, "root/sign-intermediate"),
                token,
                body,
                VaultPKISignCertificateRequestResult.class);
    }

    public Uni<VaultPKIGenerateIntermediateCSRResult> generateIntermediateCSR(String token, String mount, String type,
            VaultPKIGenerateIntermediateCSRBody body) {
        return vaultClient.post(getPath(mount, "intermediate/generate/" + type),
                token,
                body,
                VaultPKIGenerateIntermediateCSRResult.class);
    }

    public Uni<Void> setSignedIntermediateCA(String token, String mount, VaultPKISetSignedIntermediateCABody body) {
        return vaultClient.post(getPath(mount, "intermediate/set-signed"), token, body, 204);
    }

    public Uni<Void> tidy(String token, String mount, VaultPKITidyBody body) {
        return vaultClient.post(getPath(mount, "tidy"), token, body, 202);
    }

    public Uni<Void> configURLs(String token, String mount, VaultPKIConfigURLsData body) {
        return vaultClient.post(getPath(mount, "config/urls"), token, body, 204);
    }

    public Uni<VaultPKIConfigURLsResult> readURLs(String token, String mount) {
        return vaultClient.get(getPath(mount, "config/urls"), token, VaultPKIConfigURLsResult.class);
    }

    public Uni<Void> configCRL(String token, String mount, VaultPKIConfigCRLData body) {
        return vaultClient.post(getPath(mount, "config/crl"), token, body, 204);
    }

    public Uni<VaultPKIConfigCRLResult> readCRL(String token, String mount) {
        return vaultClient.get(getPath(mount, "config/crl"), token, VaultPKIConfigCRLResult.class);
    }
}

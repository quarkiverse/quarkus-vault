package io.quarkus.vault;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.pki.CAChainData;
import io.quarkus.vault.pki.CRLData;
import io.quarkus.vault.pki.CertificateData;
import io.quarkus.vault.pki.ConfigCRLOptions;
import io.quarkus.vault.pki.ConfigURLsOptions;
import io.quarkus.vault.pki.DataFormat;
import io.quarkus.vault.pki.GenerateCertificateOptions;
import io.quarkus.vault.pki.GenerateIntermediateCSROptions;
import io.quarkus.vault.pki.GenerateRootOptions;
import io.quarkus.vault.pki.GeneratedCertificate;
import io.quarkus.vault.pki.GeneratedIntermediateCSRResult;
import io.quarkus.vault.pki.GeneratedRootCertificate;
import io.quarkus.vault.pki.RoleOptions;
import io.quarkus.vault.pki.SignIntermediateCAOptions;
import io.quarkus.vault.pki.SignedCertificate;
import io.quarkus.vault.pki.TidyOptions;

/**
 * A service that interacts with Hashicorp's Vault PKI secret engine to issue certificates & manage certificate
 * authorities.
 *
 * @implNote Wrapper for reactive engine. Request timeouts are accounted for in Vault client.
 *
 * @see <a href="https://www.vaultproject.io/docs/secrets/pki">PKI</a>
 */
@ApplicationScoped
public class VaultPKISecretEngine {

    private final VaultPKISecretReactiveEngine engine;

    @Inject
    public VaultPKISecretEngine(VaultPKISecretReactiveEngine engine) {
        this.engine = engine;
    }

    /**
     * Retrieves the engine's CA certificate (PEM encoded).
     *
     * @return Certificate authority certificate.
     */
    public CertificateData.PEM getCertificateAuthority() {
        return engine.getCertificateAuthority().await().indefinitely();
    }

    /**
     * Retrieves the engine's CA certificate.
     *
     * @param format Format of the returned certificate data.
     * @return Certificate authority certificate.
     */
    public CertificateData getCertificateAuthority(DataFormat format) {
        return engine.getCertificateAuthority(format).await().indefinitely();
    }

    /**
     * Configures the engine's CA.
     *
     * @param pemBundle PEM encoded bundle including the CA, with optional chain, and private key.
     */
    public void configCertificateAuthority(String pemBundle) {
        engine.configCertificateAuthority(pemBundle).await().indefinitely();
    }

    /**
     * Configures engine's URLs for issuing certificates, CRL distribution points, and OCSP servers.
     *
     * @param options URL options.
     */
    public void configURLs(ConfigURLsOptions options) {
        engine.configURLs(options).await().indefinitely();
    }

    /**
     * Read engine's configured URLs for issuing certificates, CRL distribution points, and OCSP servers.
     *
     * @return URL options.
     */
    public ConfigURLsOptions readURLsConfig() {
        return engine.readURLsConfig().await().indefinitely();
    }

    /**
     * Configures engine's CRL.
     *
     * @param options CRL options.
     */
    public void configCRL(ConfigCRLOptions options) {
        engine.configCRL(options).await().indefinitely();
    }

    /**
     * Read engine's CRL configuration.
     *
     * @return URL options.
     */
    public ConfigCRLOptions readCRLConfig() {
        return engine.readCRLConfig().await().indefinitely();
    }

    /**
     * Retrieves the engine's CA chain (PEM encoded).
     *
     * @return Certificate authority chain.
     */
    public CAChainData.PEM getCertificateAuthorityChain() {
        return engine.getCertificateAuthorityChain().await().indefinitely();
    }

    /**
     * Retrieves the engine's CRL (PEM encoded).
     *
     * @return Certificate revocation list.
     */
    public CRLData.PEM getCertificateRevocationList() {
        return engine.getCertificateRevocationList().await().indefinitely();
    }

    /**
     * Retrieves the engine's CRL.
     *
     * @param format Format of the returned crl data.
     * @return Certificate revocation list.
     */
    public CRLData getCertificateRevocationList(DataFormat format) {
        return engine.getCertificateRevocationList(format).await().indefinitely();
    }

    /**
     * Forces a rotation of the associated CRL.
     */
    public boolean rotateCertificateRevocationList() {
        return engine.rotateCertificateRevocationList().await().indefinitely();
    }

    /**
     * List all issued certificate serial numbers.
     *
     * @return List of certificate serialize numbers.
     */
    public List<String> getCertificates() {
        return engine.getCertificates().await().indefinitely();
    }

    /**
     * Retrieve a specific certificate (PEM encoded).
     *
     * @param serial Serial number of certificate.
     * @return Certificate or null if no certificate exists.
     */
    public CertificateData.PEM getCertificate(String serial) {
        return engine.getCertificate(serial).await().indefinitely();
    }

    /**
     * Generates a public/private key pair and certificate issued from the engine's CA using the
     * provided options.
     *
     * @param role Name of role used to create certificate.
     * @param options Certificate generation options.
     * @return Generated certificate and private key.
     */
    public GeneratedCertificate generateCertificate(String role, GenerateCertificateOptions options) {
        return engine.generateCertificate(role, options).await().indefinitely();
    }

    /**
     * Generates a certificate issued from the engine's CA using the provided Certificate Signing Request and options.
     *
     * @param role Name of role used to create certificate.
     * @param pemSigningRequest Certificate Signing Request (PEM encoded).
     * @param options Certificate generation options.
     * @return Generated certificate.
     */
    public SignedCertificate signRequest(String role, String pemSigningRequest, GenerateCertificateOptions options) {
        return engine.signRequest(role, pemSigningRequest, options).await().indefinitely();
    }

    /**
     * Revokes a certificate.
     *
     * @param serialNumber Serial number of certificate.
     * @return Time of certificates revocation.
     */
    public OffsetDateTime revokeCertificate(String serialNumber) {
        return engine.revokeCertificate(serialNumber).await().indefinitely();
    }

    /**
     * Updates, or creates, a role.
     *
     * @param role Name of role.
     * @param options Options for role.
     */
    public void updateRole(String role, RoleOptions options) {
        engine.updateRole(role, options).await().indefinitely();
    }

    /**
     * Retrieve current options for a role.
     *
     * @param role Name of role.
     * @return Options for the role or null if role does not exist.
     */
    public RoleOptions getRole(String role) {
        return engine.getRole(role).await().indefinitely();
    }

    /**
     * Lists existing role names.
     *
     * @return List of role names.
     */
    public List<String> getRoles() {
        return engine.getRoles().await().indefinitely();
    }

    /**
     * Deletes a role.
     *
     * @param role Name of role.
     */
    public void deleteRole(String role) {
        engine.deleteRole(role).await().indefinitely();
    }

    /**
     * Generates a self-signed root as the engine's CA.
     *
     * @param options Generation options.
     * @return Generated root certificate.
     */
    public GeneratedRootCertificate generateRoot(GenerateRootOptions options) {
        return engine.generateRoot(options).await().indefinitely();
    }

    /**
     * Deletes the engine's current CA.
     */
    public void deleteRoot() {
        engine.deleteRoot().await().indefinitely();
    }

    /**
     * Generates an intermediate CA certificate issued from the engine's CA using the provided Certificate Signing
     * Request and options.
     *
     * @param pemSigningRequest Certificate Signing Request (PEM encoded).
     * @param options Signing options.
     * @return Generated certificate.
     */
    public SignedCertificate signIntermediateCA(String pemSigningRequest, SignIntermediateCAOptions options) {
        return engine.signIntermediateCA(pemSigningRequest, options).await().indefinitely();
    }

    /**
     * Generates a Certificate Signing Request and private key for the engine's CA.
     *
     * Use this to generate a CSR and for the engine's CA that can be used by another
     * CA to issue an intermediate CA certificate. After generating the intermediate CA
     * {@link #setSignedIntermediateCA(String)} must be used to set the engine's CA certificate.
     *
     * This will overwrite any previously existing CA private key for the engine.
     *
     * @see #setSignedIntermediateCA(String)
     * @param options Options for CSR generation.
     * @return Generated CSR and, if key export is enabled, private key.
     */
    public GeneratedIntermediateCSRResult generateIntermediateCSR(GenerateIntermediateCSROptions options) {
        return engine.generateIntermediateCSR(options).await().indefinitely();
    }

    /**
     * Sets the engine's intermediate CA certificate, signed by another CA.
     *
     * After generating a CSR (via {@link #generateIntermediateCSR(GenerateIntermediateCSROptions)}),
     * this method must be used to set the engine's CA.
     *
     * @see #generateIntermediateCSR(GenerateIntermediateCSROptions)
     * @param pemCert Signed certificate (PEM encoded).
     */
    public void setSignedIntermediateCA(String pemCert) {
        engine.setSignedIntermediateCA(pemCert).await().indefinitely();
    }

    /**
     * Tidy up the storage backend and/or CRL by removing certificates that have expired and are past a certain buffer
     * period beyond their expiration time.
     *
     * @param options Tidy options.
     */
    public void tidy(TidyOptions options) {
        engine.tidy(options).await().indefinitely();
    }

}

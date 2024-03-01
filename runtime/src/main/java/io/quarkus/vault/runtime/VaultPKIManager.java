package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.DurationHelper.*;
import static io.quarkus.vault.runtime.VaultPKIManagerFactory.PKI_ENGINE_NAME;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultPKISecretReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.api.secrets.pki.*;
import io.quarkus.vault.pki.CAChainData;
import io.quarkus.vault.pki.CRLData;
import io.quarkus.vault.pki.CSRData;
import io.quarkus.vault.pki.CertificateData;
import io.quarkus.vault.pki.CertificateExtendedKeyUsage;
import io.quarkus.vault.pki.CertificateKeyType;
import io.quarkus.vault.pki.CertificateKeyUsage;
import io.quarkus.vault.pki.ConfigCRLOptions;
import io.quarkus.vault.pki.ConfigURLsOptions;
import io.quarkus.vault.pki.DataFormat;
import io.quarkus.vault.pki.GenerateCertificateOptions;
import io.quarkus.vault.pki.GenerateIntermediateCSROptions;
import io.quarkus.vault.pki.GenerateRootOptions;
import io.quarkus.vault.pki.GeneratedCertificate;
import io.quarkus.vault.pki.GeneratedIntermediateCSRResult;
import io.quarkus.vault.pki.GeneratedRootCertificate;
import io.quarkus.vault.pki.PrivateKeyData;
import io.quarkus.vault.pki.PrivateKeyEncoding;
import io.quarkus.vault.pki.RoleOptions;
import io.quarkus.vault.pki.SignIntermediateCAOptions;
import io.quarkus.vault.pki.SignedCertificate;
import io.quarkus.vault.pki.TidyOptions;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.unchecked.Unchecked;

@ApplicationScoped
public class VaultPKIManager implements VaultPKISecretReactiveEngine {

    private final VaultSecretsPKI pki;

    @Inject
    public VaultPKIManager(VaultClient vaultClient) {
        this(vaultClient, PKI_ENGINE_NAME);
    }

    VaultPKIManager(VaultClient vaultClient, String mount) {
        this.pki = vaultClient.secrets().pki(mount);
    }

    @Override
    public Uni<CertificateData.PEM> getCertificateAuthority() {
        return getCertificateAuthority(DataFormat.PEM).map(r -> (CertificateData.PEM) r);
    }

    @Override
    public Uni<CertificateData> getCertificateAuthority(DataFormat format) {
        return Uni.createFrom().completionStage(pki.readIssuerCaCert())
                .map(Unchecked.function(result -> {
                    var certData = new CertificateData.PEM(result.getCertificate());
                    if (format == DataFormat.PEM) {
                        return certData;
                    } else {
                        return new CertificateData.DER(certData.getCertificate().getEncoded());
                    }
                }));
    }

    @Override
    public Uni<Void> configCertificateAuthority(String pemBundle) {
        return Uni.createFrom().completionStage(pki.configCa(pemBundle)).map(r -> null);
    }

    @Override
    public Uni<Void> configURLs(ConfigURLsOptions options) {

        var params = new VaultSecretsPKIConfigUrlsParams()
                .setIssuingCertificates(options.issuingCertificates)
                .setCrlDistributionPoints(options.crlDistributionPoints)
                .setOcspServers(options.ocspServers);

        return Uni.createFrom().completionStage(pki.configUrls(params));
    }

    @Override
    public Uni<ConfigURLsOptions> readURLsConfig() {
        return Uni.createFrom().completionStage(pki.readUrlsConfig())
                .map(result -> {
                    var options = new ConfigURLsOptions();
                    options.issuingCertificates = result.getIssuingCertificates();
                    options.crlDistributionPoints = result.getCrlDistributionPoints();
                    options.ocspServers = result.getOcspServers();
                    return options;
                });
    }

    @Override
    public Uni<Void> configCRL(ConfigCRLOptions options) {

        var params = new VaultSecretsPKIConfigCrlParams()
                .setExpiry(fromVaultDuration(options.expiry))
                .setDisable(options.disable);

        return Uni.createFrom().completionStage(pki.configCrl(params));
    }

    @Override
    public Uni<ConfigCRLOptions> readCRLConfig() {
        return Uni.createFrom().completionStage(pki.readCrlConfig())
                .map(result -> {
                    var options = new ConfigCRLOptions();
                    options.expiry = toVaultDuration(result.getExpiry());
                    options.disable = result.isDisable();
                    return options;
                });
    }

    @Override
    public Uni<CAChainData.PEM> getCertificateAuthorityChain() {
        return Uni.createFrom().completionStage(pki.readIssuerCaChain()).map(CAChainData.PEM::new);
    }

    @Override
    public Uni<CRLData.PEM> getCertificateRevocationList() {
        return getCertificateRevocationList(DataFormat.PEM).map(r -> (CRLData.PEM) r);
    }

    @Override
    public Uni<CRLData> getCertificateRevocationList(DataFormat format) {
        return Uni.createFrom().completionStage(pki.readIssuerCrl())
                .map(Unchecked.function(result -> {
                    var crlData = new CRLData.PEM(result);
                    if (format == DataFormat.PEM) {
                        return crlData;
                    } else {
                        return new CRLData.DER(crlData.getCRL().getEncoded());
                    }
                }));
    }

    @Override
    public Uni<Boolean> rotateCertificateRevocationList() {
        return Uni.createFrom().completionStage(pki.rotateCrl()).map(VaultSecretsPKIRotateCrlResultData::isSuccess);
    }

    @Override
    public Uni<List<String>> getCertificates() {
        return Uni.createFrom().completionStage(pki.listCertificates())
                .map(results -> {
                    var serials = new ArrayList<String>();
                    for (String serial : results) {
                        serials.add(serial.replaceAll("-", ":"));
                    }
                    return serials;
                });
    }

    @Override
    public Uni<CertificateData.PEM> getCertificate(String serial) {
        return Uni.createFrom().completionStage(pki.readCertificate(serial))
                .map(result -> new CertificateData.PEM(result.getCertificate()));
    }

    @Override
    public Uni<GeneratedCertificate> generateCertificate(String role, GenerateCertificateOptions options) {

        var params = new VaultSecretsPKIIssueParams()
                .setFormat(dataFormatToFormat(options.format))
                .setPrivateKeyFormat(privateKeyFormat(options.privateKeyEncoding))
                .setCommonName(options.subjectCommonName)
                .setAltNames(options.subjectAlternativeNames)
                .setIpSans(options.ipSubjectAlternativeNames)
                .setUriSans(options.uriSubjectAlternativeNames)
                .setOtherSans(options.otherSubjectAlternativeNames)
                .setTtl(fromVaultDuration(options.timeToLive))
                .setExcludeCommonNameFromSubjectAlternativeNames(options.excludeCommonNameFromSubjectAlternativeNames);

        return Uni.createFrom().completionStage(pki.issue(role, params))
                .map(data -> {

                    GeneratedCertificate result = new GeneratedCertificate();
                    result.certificate = createCertificateData(data.getCertificate(), params.getFormat());
                    result.issuingCA = createCertificateData(data.getIssuingCa(), params.getFormat());
                    result.caChain = createCertificateDataList(data.getCaChain(), params.getFormat());
                    result.serialNumber = data.getSerialNumber();
                    result.privateKeyType = stringToCertificateKeyType(data.getPrivateKeyType());
                    result.privateKey = createPrivateKeyData(data.getPrivateKey(), params.getFormat(),
                            params.getPrivateKeyFormat());
                    return result;
                });
    }

    @Override
    public Uni<SignedCertificate> signRequest(String role, String pemSigningRequest, GenerateCertificateOptions options) {

        var params = new VaultSecretsPKISignParams()
                .setFormat(dataFormatToFormat(options.format))
                .setCsr(pemSigningRequest)
                .setCommonName(options.subjectCommonName)
                .setAltNames(options.subjectAlternativeNames)
                .setIpSans(options.ipSubjectAlternativeNames)
                .setUriSans(options.uriSubjectAlternativeNames)
                .setOtherSans(options.otherSubjectAlternativeNames)
                .setTtl(fromVaultDuration(options.timeToLive))
                .setExcludeCommonNameFromSubjectAlternativeNames(options.excludeCommonNameFromSubjectAlternativeNames);

        return Uni.createFrom().completionStage(pki.sign(role, params))
                .map(data -> {

                    SignedCertificate result = new SignedCertificate();
                    result.certificate = createCertificateData(data.getCertificate(), params.getFormat());
                    result.issuingCA = createCertificateData(data.getIssuingCa(), params.getFormat());
                    result.caChain = createCertificateDataList(data.getCaChain(), params.getFormat());
                    result.serialNumber = data.getSerialNumber();
                    return result;
                });
    }

    @Override
    public Uni<OffsetDateTime> revokeCertificate(String serialNumber) {

        var params = new VaultSecretsPKIRevokeParams()
                .setSerialNumber(serialNumber);

        return Uni.createFrom().completionStage(pki.revoke(params))
                .map(VaultSecretsPKIRevokeResultData::getRevocationTime);
    }

    @Override
    public Uni<Void> updateRole(String role, RoleOptions options) {

        var params = new VaultSecretsPKIUpdateRoleParams()
                .setTtl(fromVaultDuration(options.timeToLive))
                .setMaxTtl(fromVaultDuration(options.maxTimeToLive))
                .setAllowLocalhost(options.allowLocalhost)
                .setAllowedDomains(options.allowedDomains)
                .setAllowedDomainsTemplate(options.allowTemplatesInAllowedDomains)
                .setAllowBareDomains(options.allowBareDomains)
                .setAllowSubdomains(options.allowSubdomains)
                .setAllowGlobDomains(options.allowGlobsInAllowedDomains)
                .setAllowAnyName(options.allowAnyName)
                .setEnforceHostnames(options.enforceHostnames)
                .setAllowIpSans(options.allowIpSubjectAlternativeNames)
                .setAllowedUriSans(options.allowedUriSubjectAlternativeNames)
                .setAllowedOtherSans(options.allowedOtherSubjectAlternativeNames)
                .setServerFlag(options.serverFlag)
                .setClientFlag(options.clientFlag)
                .setCodeSigningFlag(options.codeSigningFlag)
                .setEmailProtectionFlag(options.emailProtectionFlag)
                .setKeyType(
                        options.keyType != null ? VaultSecretsPKIKeyType.from(options.keyType.name().toLowerCase(Locale.ROOT))
                                : null)
                .setKeyBits(options.keyBits != null ? VaultSecretsPKIKeyBits.fromBits(options.keyBits) : null)
                .setKeyUsage(mapKeyUsagesToClient(options.keyUsages))
                .setExtKeyUsage(mapExtKeyUsagesToClient(options.extendedKeyUsages))
                .setExtKeyUsageOids(options.extendedKeyUsageOIDs)
                .setUseCsrCommonName(options.useCSRCommonName)
                .setUseCsrSans(options.useCSRSubjectAlternativeNames)
                .setOrganization(commaStringToStringList(options.subjectOrganization))
                .setOu(commaStringToStringList(options.subjectOrganizationalUnit))
                .setStreetAddress(commaStringToStringList(options.subjectStreetAddress))
                .setPostalCode(commaStringToStringList(options.subjectPostalCode))
                .setLocality(commaStringToStringList(options.subjectLocality))
                .setProvince(commaStringToStringList(options.subjectProvince))
                .setCountry(commaStringToStringList(options.subjectCountry))
                .setAllowedSerialNumbers(options.allowedSubjectSerialNumbers)
                .setGenerateLease(options.generateLease)
                .setNoStore(options.noStore)
                .setRequireCn(options.requireCommonName)
                .setPolicyIdentifiers(options.policyOIDs)
                .setBasicConstraintsValidForNonCa(options.basicConstraintsValidForNonCA)
                .setNotBefore(fromVaultDuration(options.notBeforeDuration));

        return Uni.createFrom().completionStage(pki.updateRole(role, params));
    }

    @Override
    public Uni<RoleOptions> getRole(String role) {
        return Uni.createFrom().completionStage(pki.readRole(role))
                .map(info -> {
                    RoleOptions result = new RoleOptions();
                    result.timeToLive = toStringDurationSeconds(info.getTtl());
                    result.maxTimeToLive = toStringDurationSeconds(info.getMaxTtl());
                    result.allowLocalhost = info.isAllowLocalhost();
                    result.allowedDomains = info.getAllowedDomains();
                    result.allowTemplatesInAllowedDomains = info.isAllowedDomainsTemplate();
                    result.allowBareDomains = info.isAllowBareDomains();
                    result.allowSubdomains = info.isAllowSubdomains();
                    result.allowGlobsInAllowedDomains = info.isAllowGlobDomains();
                    result.allowAnyName = info.isAllowAnyName();
                    result.enforceHostnames = info.isEnforceHostnames();
                    result.allowIpSubjectAlternativeNames = info.isAllowIpSans();
                    result.allowedUriSubjectAlternativeNames = info.getAllowedUriSans();
                    result.allowedOtherSubjectAlternativeNames = info.getAllowedOtherSans();
                    result.serverFlag = info.isServerFlag();
                    result.clientFlag = info.isClientFlag();
                    result.codeSigningFlag = info.isCodeSigningFlag();
                    result.emailProtectionFlag = info.isEmailProtectionFlag();
                    result.keyType = stringToCertificateKeyType(info.getKeyType());
                    result.keyBits = info.getKeyBits().getBits();
                    result.keyUsages = mapKeyUsagesFromClient(info.getKeyUsage());
                    result.extendedKeyUsages = mapExtKeyUsagesFromClient(info.getExtKeyUsage());
                    result.extendedKeyUsageOIDs = info.getExtKeyUsageOids();
                    result.useCSRCommonName = info.isUseCsrCommonName();
                    result.useCSRSubjectAlternativeNames = info.isUseCsrSans();
                    result.subjectOrganization = stringListToCommaString(info.getOrganization());
                    result.subjectOrganizationalUnit = stringListToCommaString(
                            info.getOu());
                    result.subjectStreetAddress = stringListToCommaString(info.getStreetAddress());
                    result.subjectPostalCode = stringListToCommaString(info.getPostalCode());
                    result.subjectLocality = stringListToCommaString(info.getLocality());
                    result.subjectProvince = stringListToCommaString(info.getProvince());
                    result.subjectCountry = stringListToCommaString(info.getCountry());
                    result.allowedSubjectSerialNumbers = info.getAllowedSerialNumbers();
                    result.generateLease = info.isGenerateLease();
                    result.noStore = info.isNoStore();
                    result.requireCommonName = info.isRequireCn();
                    result.policyOIDs = info.getPolicyIdentifiers();
                    result.basicConstraintsValidForNonCA = info.isBasicConstraintsValidForNonCa();
                    result.notBeforeDuration = toStringDurationSeconds(info.getNotBefore());
                    return result;
                });
    }

    @Override
    public Uni<List<String>> getRoles() {
        return Uni.createFrom().completionStage(pki.listRoles())
                .onFailure(VaultClientException.class).recoverWithUni(x -> {
                    VaultClientException vx = (VaultClientException) x;
                    // Translate 404 to empty list
                    if (vx.getStatus() == 404) {
                        return Uni.createFrom().item(emptyList());
                    } else {
                        return Uni.createFrom().failure(x);
                    }
                });
    }

    @Override
    public Uni<Void> deleteRole(String role) {
        return Uni.createFrom().completionStage(pki.deleteRole(role));
    }

    @Override
    public Uni<GeneratedRootCertificate> generateRoot(GenerateRootOptions options) {

        var params = new VaultSecretsPKIGenerateRootParams()
                .setFormat(dataFormatToFormat(options.format))
                .setPrivateKeyFormat(privateKeyFormat(options.privateKeyEncoding))
                .setCommonName(options.subjectCommonName)
                .setAltNames(options.subjectAlternativeNames)
                .setIpSans(options.ipSubjectAlternativeNames)
                .setUriSans(options.uriSubjectAlternativeNames)
                .setOtherSans(options.otherSubjectAlternativeNames)
                .setTtl(fromVaultDuration(options.timeToLive))
                .setKeyType(
                        options.keyType != null ? VaultSecretsPKIKeyType.from(options.keyType.name().toLowerCase(Locale.ROOT))
                                : null)
                .setKeyBits(VaultSecretsPKIKeyBits.fromBits(options.keyBits))
                .setMaxPathLength(options.maxPathLength)
                .setExcludeCommonNameFromSubjectAlternativeNames(options.excludeCommonNameFromSubjectAlternativeNames)
                .setPermittedDnsDomains(options.permittedDnsDomains)
                .setOrganization(commaStringToStringList(options.subjectOrganization))
                .setOu(commaStringToStringList(options.subjectOrganizationalUnit))
                .setStreetAddress(commaStringToStringList(options.subjectStreetAddress))
                .setPostalCode(commaStringToStringList(options.subjectPostalCode))
                .setLocality(commaStringToStringList(options.subjectLocality))
                .setProvince(commaStringToStringList(options.subjectProvince))
                .setCountry(commaStringToStringList(options.subjectCountry))
                .setSerialNumber(options.subjectSerialNumber);

        return Uni.createFrom()
                .completionStage(pki.generateRoot(
                        options.exportPrivateKey ? VaultSecretsPKIManageType.EXPORTED : VaultSecretsPKIManageType.INTERNAL,
                        params))
                .map(data -> {

                    GeneratedRootCertificate result = new GeneratedRootCertificate();
                    result.certificate = createCertificateData(data.getCertificate(), params.getFormat());
                    result.issuingCA = createCertificateData(data.getIssuingCa(), params.getFormat());
                    result.serialNumber = data.getSerialNumber();
                    result.privateKeyType = stringToCertificateKeyType(data.getPrivateKeyType());
                    result.privateKey = createPrivateKeyData(data.getPrivateKey(), params.getFormat(),
                            params.getPrivateKeyFormat());
                    return result;
                });
    }

    @Override
    public Uni<Void> deleteRoot() {
        return Uni.createFrom().completionStage(pki.deleteIssuer("default"));
    }

    @Override
    public Uni<SignedCertificate> signIntermediateCA(String pemSigningRequest, SignIntermediateCAOptions options) {

        var params = new VaultSecretsPKISignIntermediateParams()
                .setFormat(dataFormatToFormat(options.format))
                .setCsr(pemSigningRequest)
                .setCommonName(options.subjectCommonName)
                .setAltNames(options.subjectAlternativeNames)
                .setIpSans(options.ipSubjectAlternativeNames)
                .setUriSans(options.uriSubjectAlternativeNames)
                .setOtherSans(options.otherSubjectAlternativeNames)
                .setTtl(fromVaultDuration(options.timeToLive))
                .setMaxPathLength(options.maxPathLength)
                .setExcludeCommonNameFromSubjectAlternativeNames(options.excludeCommonNameFromSubjectAlternativeNames)
                .setUseCsrValues(options.useCSRValues)
                .setPermittedDnsDomains(options.permittedDnsDomains)
                .setOrganization(commaStringToStringList(options.subjectOrganization))
                .setOu(commaStringToStringList(options.subjectOrganizationalUnit))
                .setStreetAddress(commaStringToStringList(options.subjectStreetAddress))
                .setPostalCode(commaStringToStringList(options.subjectPostalCode))
                .setLocality(commaStringToStringList(options.subjectLocality))
                .setProvince(commaStringToStringList(options.subjectProvince))
                .setCountry(commaStringToStringList(options.subjectCountry))
                .setSerialNumber(options.subjectSerialNumber);

        return Uni.createFrom().completionStage(pki.signIntermediate(params))
                .map(data -> {

                    SignedCertificate result = new SignedCertificate();
                    result.certificate = createCertificateData(data.getCertificate(), params.getFormat());
                    result.issuingCA = createCertificateData(data.getIssuingCa(), params.getFormat());
                    result.caChain = createCertificateDataList(data.getCaChain(), params.getFormat());
                    result.serialNumber = data.getSerialNumber();
                    return result;
                });
    }

    @Override
    public Uni<GeneratedIntermediateCSRResult> generateIntermediateCSR(GenerateIntermediateCSROptions options) {

        var params = new VaultSecretsPKIGenerateCsrParams()
                .setFormat(dataFormatToFormat(options.format))
                .setPrivateKeyFormat(privateKeyFormat(options.privateKeyEncoding))
                .setCommonName(options.subjectCommonName)
                .setAltNames(options.subjectAlternativeNames)
                .setIpSans(options.ipSubjectAlternativeNames)
                .setUriSans(options.uriSubjectAlternativeNames)
                .setOtherSans(options.otherSubjectAlternativeNames)
                .setKeyType(
                        options.keyType != null ? VaultSecretsPKIKeyType.from(options.keyType.name().toLowerCase(Locale.ROOT))
                                : null)
                .setKeyBits(VaultSecretsPKIKeyBits.fromBits(options.keyBits))
                .setExcludeCommonNameFromSubjectAlternativeNames(options.excludeCommonNameFromSubjectAlternativeNames)
                .setOrganization(commaStringToStringList(options.subjectOrganization))
                .setOu(commaStringToStringList(options.subjectOrganizationalUnit))
                .setStreetAddress(commaStringToStringList(options.subjectStreetAddress))
                .setPostalCode(commaStringToStringList(options.subjectPostalCode))
                .setLocality(commaStringToStringList(options.subjectLocality))
                .setProvince(commaStringToStringList(options.subjectProvince))
                .setCountry(commaStringToStringList(options.subjectCountry))
                .setSerialNumber(options.subjectSerialNumber);

        return Uni.createFrom()
                .completionStage(pki.generateIntermediateCsr(
                        options.exportPrivateKey ? VaultSecretsPKIManageType.EXPORTED : VaultSecretsPKIManageType.INTERNAL,
                        params))
                .map(data -> {

                    GeneratedIntermediateCSRResult result = new GeneratedIntermediateCSRResult();
                    result.csr = createCSRData(data.getCsr(), params.getFormat());
                    result.privateKeyType = stringToCertificateKeyType(data.getPrivateKeyType());
                    result.privateKey = createPrivateKeyData(data.getPrivateKey(), params.getFormat(),
                            params.getPrivateKeyFormat());
                    return result;
                });
    }

    @Override
    public Uni<Void> setSignedIntermediateCA(String pemCert) {
        return Uni.createFrom().completionStage(pki.setSignedIntermediate(pemCert)).map(r -> null);
    }

    @Override
    public Uni<Void> tidy(TidyOptions options) {

        var params = new VaultSecretsPKITidyParams()
                .setTidyCertStore(options.tidyCertStore)
                .setTidyRevokedCerts(options.tidyRevokedCerts)
                .setSafetyBuffer(fromVaultDuration(options.safetyBuffer));

        return Uni.createFrom().completionStage(pki.tidy(params));
    }

    private String stringListToCommaString(List<String> values) {
        if (values == null) {
            return null;
        }
        return String.join(",", values);
    }

    private List<String> commaStringToStringList(String value) {
        if (value == null) {
            return null;
        }
        return asList(value.split(","));
    }

    private CertificateKeyType stringToCertificateKeyType(VaultSecretsPKIKeyType value) {
        if (value == null) {
            return null;
        }
        return CertificateKeyType.valueOf(value.getValue().toUpperCase(Locale.ROOT));
    }

    private VaultSecretsPKIFormat dataFormatToFormat(DataFormat format) {
        if (format == null) {
            return VaultSecretsPKIFormat.PEM;
        }
        return VaultSecretsPKIFormat.from(format.name().toLowerCase(Locale.ROOT));
    }

    private VaultSecretsPKIFormat nonNullFormat(VaultSecretsPKIFormat format) {
        if (format == null) {
            return VaultSecretsPKIFormat.PEM;
        }
        return format;
    }

    private VaultSecretsPKIPrivateKeyFormat privateKeyFormat(PrivateKeyEncoding privateKeyEncoding) {
        if (privateKeyEncoding == null || privateKeyEncoding == PrivateKeyEncoding.PKCS8) {
            return VaultSecretsPKIPrivateKeyFormat.PKCS8;
        }
        return VaultSecretsPKIPrivateKeyFormat.DER;
    }

    private CertificateData createCertificateData(String data, VaultSecretsPKIFormat format) {
        if (data == null) {
            return null;
        }
        return switch (nonNullFormat(format)) {
            case DER -> new CertificateData.DER(Base64.getDecoder().decode(data));
            case PEM -> new CertificateData.PEM(data);
            default -> throw new VaultException("Unsupported certificate format");
        };
    }

    private List<CertificateData> createCertificateDataList(List<String> datas, VaultSecretsPKIFormat format) {
        if (datas == null) {
            return null;
        }
        List<CertificateData> result = new ArrayList<>(datas.size());
        for (String data : datas) {
            result.add(createCertificateData(data, format));
        }
        return result;
    }

    private CSRData createCSRData(String data, VaultSecretsPKIFormat format) {
        if (data == null) {
            return null;
        }
        return switch (nonNullFormat(format)) {
            case DER -> new CSRData.DER(Base64.getDecoder().decode(data));
            case PEM -> new CSRData.PEM(data);
            default -> throw new VaultException("Unsupported certification request format");
        };
    }

    private PrivateKeyData createPrivateKeyData(String data, VaultSecretsPKIFormat format,
            VaultSecretsPKIPrivateKeyFormat privateKeyFormat) {
        if (data == null) {
            return null;
        }
        boolean pkcs8 = privateKeyFormat == VaultSecretsPKIPrivateKeyFormat.PKCS8;
        return switch (nonNullFormat(format)) {
            case DER -> new PrivateKeyData.DER(Base64.getDecoder().decode(data), pkcs8);
            case PEM -> new PrivateKeyData.PEM(data, pkcs8);
            default -> throw new VaultException("Unsupported private key format");
        };
    }

    private static List<VaultSecretsPKIKeyUsage> mapKeyUsagesToClient(List<CertificateKeyUsage> keyUsages) {
        if (keyUsages == null) {
            return null;
        }
        return keyUsages.stream().map(e -> VaultSecretsPKIKeyUsage.from(e.name())).collect(toList());
    }

    private static List<CertificateKeyUsage> mapKeyUsagesFromClient(List<VaultSecretsPKIKeyUsage> keyUsages) {
        if (keyUsages == null) {
            return null;
        }
        return keyUsages.stream().map(e -> CertificateKeyUsage.valueOf(e.getValue())).collect(toList());
    }

    private static List<VaultSecretsPKIExtKeyUsage> mapExtKeyUsagesToClient(List<CertificateExtendedKeyUsage> keyUsages) {
        if (keyUsages == null) {
            return null;
        }
        return keyUsages.stream().map(e -> VaultSecretsPKIExtKeyUsage.from(e.name())).collect(toList());
    }

    private static List<CertificateExtendedKeyUsage> mapExtKeyUsagesFromClient(List<VaultSecretsPKIExtKeyUsage> keyUsages) {
        if (keyUsages == null) {
            return null;
        }
        return keyUsages.stream().map(e -> CertificateExtendedKeyUsage.valueOf(e.getValue())).collect(toList());
    }
}

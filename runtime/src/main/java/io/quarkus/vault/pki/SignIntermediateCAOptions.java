package io.quarkus.vault.pki;

import java.util.List;

/**
 * Options for signing an intermediate CA certificate.
 */
public class SignIntermediateCAOptions {

    /**
     * Specifies Common Name (CN) of the subject.
     */
    private String subjectCommonName;

    /**
     * Specifies Organization (O) of the subject.
     */
    private String subjectOrganization;

    /**
     * Specifies Organizational Unit (OU) of the subject.
     */
    private String subjectOrganizationalUnit;

    /**
     * Specifies Street Address of the subject.
     */
    private String subjectStreetAddress;

    /**
     * Specifies Postal Code of the subject.
     */
    private String subjectPostalCode;

    /**
     * Specifies Locality (L) of the subject.
     */
    private String subjectLocality;

    /**
     * Specifies Province (ST) of the subject.
     */
    private String subjectProvince;

    /**
     * Specifies Country (C) of the subject.
     */
    private String subjectCountry;

    /**
     * Specifies the Serial Number (SERIALNUMBER) of the subject.
     */
    private String subjectSerialNumber;

    /**
     * Specifies Subject Alternative Names.
     * <p>
     * These can be host names or email addresses; they will be parsed into their respective fields.
     */
    private List<String> subjectAlternativeNames;

    /**
     * Flag determining if the Common Name (CN) of the subject will be included
     * by default in the Subject Alternative Names of issued certificates.
     */
    private Boolean excludeCommonNameFromSubjectAlternativeNames;

    /**
     * Specifies IP Subject Alternative Names.
     */
    private List<String> ipSubjectAlternativeNames;

    /**
     * Specifies URI Subject Alternative Names.
     */
    private List<String> uriSubjectAlternativeNames;

    /**
     * Specifies custom OID/UTF8-string Subject Alternative Names.
     * <p>
     * The format is the same as OpenSSL: <oid>;<type>:<value> where the only current valid type is UTF8.
     */
    private List<String> otherSubjectAlternativeNames;

    /**
     * Specifies time-to-live.
     * <p>
     * Value is specified as a string duration with time suffix. Hour is the largest supported suffix.
     */
    private String timeToLive;

    /**
     * Specifies the maximum path length for generated certificate.
     */
    private Integer maxPathLength;

    /**
     * Flag determining if CSR values are used instead of configured default values.
     * <p>
     * Enables the following handling:
     * <ul>
     * <li>Subject information, including names and alternate names, will be preserved from the CSR.</li>
     * <li>Any key usages (for instance, non-repudiation) requested in the CSR will be added to the set of CA key
     * usages.</li>
     * <li>Extensions requested in the CSR will be copied into the issued certificate.</li>
     * </ul>
     */
    private Boolean useCSRValues;

    /**
     * DNS domains for which certificates are allowed to be issued or signed by this CA certificate. Subdomains
     * are allowed, as per RFC.
     */
    private List<String> permittedDnsDomains;

    /**
     * Specifies returned format of certificate data. If unspecified it defaults
     * to {@link DataFormat#PEM}
     */
    private DataFormat format;

    public SignIntermediateCAOptions subjectCommonName(String subjectCommonName) {
        this.subjectCommonName = subjectCommonName;
        return this;
    }

    public SignIntermediateCAOptions subjectOrganization(String subjectOrganization) {
        this.subjectOrganization = subjectOrganization;
        return this;
    }

    public SignIntermediateCAOptions subjectOrganizationalUnit(String subjectOrganizationalUnit) {
        this.subjectOrganizationalUnit = subjectOrganizationalUnit;
        return this;
    }

    public SignIntermediateCAOptions subjectStreetAddress(String subjectStreetAddress) {
        this.subjectStreetAddress = subjectStreetAddress;
        return this;
    }

    public SignIntermediateCAOptions subjectPostalCode(String subjectPostalCode) {
        this.subjectPostalCode = subjectPostalCode;
        return this;
    }

    public SignIntermediateCAOptions subjectLocality(String subjectLocality) {
        this.subjectLocality = subjectLocality;
        return this;
    }

    public SignIntermediateCAOptions subjectProvince(String subjectProvince) {
        this.subjectProvince = subjectProvince;
        return this;
    }

    public SignIntermediateCAOptions subjectCountry(String subjectCountry) {
        this.subjectCountry = subjectCountry;
        return this;
    }

    public SignIntermediateCAOptions subjectSerialNumber(String subjectSerialNumber) {
        this.subjectSerialNumber = subjectSerialNumber;
        return this;
    }

    public SignIntermediateCAOptions subjectAlternativeNames(List<String> subjectAlternativeNames) {
        this.subjectAlternativeNames = subjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions excludeCommonNameFromSubjectAlternativeNames(
            Boolean excludeCommonNameFromSubjectAlternativeNames) {
        this.excludeCommonNameFromSubjectAlternativeNames = excludeCommonNameFromSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions ipSubjectAlternativeNames(
            List<String> ipSubjectAlternativeNames) {
        this.ipSubjectAlternativeNames = ipSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions uriSubjectAlternativeNames(
            List<String> uriSubjectAlternativeNames) {
        this.uriSubjectAlternativeNames = uriSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions otherSubjectAlternativeNames(
            List<String> otherSubjectAlternativeNames) {
        this.otherSubjectAlternativeNames = otherSubjectAlternativeNames;
        return this;
    }

    public SignIntermediateCAOptions timeToLive(String timeToLive) {
        this.timeToLive = timeToLive;
        return this;
    }

    public SignIntermediateCAOptions maxPathLength(Integer maxPathLength) {
        this.maxPathLength = maxPathLength;
        return this;
    }

    public SignIntermediateCAOptions useCSRValues(Boolean useCSRValues) {
        this.useCSRValues = useCSRValues;
        return this;
    }

    public SignIntermediateCAOptions permittedDnsDomains(List<String> permittedDnsDomains) {
        this.permittedDnsDomains = permittedDnsDomains;
        return this;
    }

    public SignIntermediateCAOptions format(DataFormat format) {
        this.format = format;
        return this;
    }

    public String subjectCommonName() {
        return subjectCommonName;
    }

    public String subjectOrganization() {
        return subjectOrganization;
    }

    public String subjectOrganizationalUnit() {
        return subjectOrganizationalUnit;
    }

    public String subjectStreetAddress() {
        return subjectStreetAddress;
    }

    public String subjectPostalCode() {
        return subjectPostalCode;
    }

    public String subjectLocality() {
        return subjectLocality;
    }

    public String subjectProvince() {
        return subjectProvince;
    }

    public String subjectCountry() {
        return subjectCountry;
    }

    public String subjectSerialNumber() {
        return subjectSerialNumber;
    }

    public List<String> subjectAlternativeNames() {
        return subjectAlternativeNames;
    }

    public Boolean excludeCommonNameFromSubjectAlternativeNames() {
        return excludeCommonNameFromSubjectAlternativeNames;
    }

    public List<String> ipSubjectAlternativeNames() {
        return ipSubjectAlternativeNames;
    }

    public List<String> uriSubjectAlternativeNames() {
        return uriSubjectAlternativeNames;
    }

    public List<String> otherSubjectAlternativeNames() {
        return otherSubjectAlternativeNames;
    }

    public String timeToLive() {
        return timeToLive;
    }

    public Integer maxPathLength() {
        return maxPathLength;
    }

    public Boolean useCSRValues() {
        return useCSRValues;
    }

    public List<String> permittedDnsDomains() {
        return permittedDnsDomains;
    }

    public DataFormat format() {
        return format;
    }
}

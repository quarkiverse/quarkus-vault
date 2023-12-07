package io.quarkus.vault.sys;

import java.util.List;
import java.util.Map;

public class VaultSecretEngineInfo {

    private String description;

    private Boolean externalEntropyAccess;

    private Boolean local;

    private Boolean sealWrap;

    private String type;

    private String pluginVersion;

    private String runningPluginVersion;

    private String runningSha256;

    public Long defaultLeaseTimeToLive;

    public Long maxLeaseTimeToLive;

    public Boolean forceNoCache;

    private Map<String, Object> options;

    private List<String> auditNonHMACRequestKeys;

    private List<String> auditNonHMACResponseKeys;

    private EngineListingVisibility listingVisibility;

    private List<String> passthroughRequestHeaders;

    private List<String> allowedResponseHeaders;

    private List<String> allowedManagedKeys;

    public Boolean getExternalEntropyAccess() {
        return externalEntropyAccess;
    }

    public Boolean getLocal() {
        return local;
    }

    public Boolean getSealWrap() {
        return sealWrap;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getRunningPluginVersion() {
        return runningPluginVersion;
    }

    public String getRunningSha256() {
        return runningSha256;
    }

    public Long getDefaultLeaseTimeToLive() {
        return defaultLeaseTimeToLive;
    }

    public Long getMaxLeaseTimeToLive() {
        return maxLeaseTimeToLive;
    }

    public Boolean getForceNoCache() {
        return forceNoCache;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public List<String> getAuditNonHMACRequestKeys() {
        return auditNonHMACRequestKeys;
    }

    public List<String> getAuditNonHMACResponseKeys() {
        return auditNonHMACResponseKeys;
    }

    public EngineListingVisibility getListingVisibility() {
        return listingVisibility;
    }

    public List<String> getPassthroughRequestHeaders() {
        return passthroughRequestHeaders;
    }

    public List<String> getAllowedResponseHeaders() {
        return allowedResponseHeaders;
    }

    public List<String> getAllowedManagedKeys() {
        return allowedManagedKeys;
    }

    public VaultSecretEngineInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public VaultSecretEngineInfo setExternalEntropyAccess(Boolean externalEntropyAccess) {
        this.externalEntropyAccess = externalEntropyAccess;
        return this;
    }

    public VaultSecretEngineInfo setLocal(Boolean local) {
        this.local = local;
        return this;
    }

    public VaultSecretEngineInfo setSealWrap(Boolean sealWrap) {
        this.sealWrap = sealWrap;
        return this;
    }

    public VaultSecretEngineInfo setType(String type) {
        this.type = type;
        return this;
    }

    public VaultSecretEngineInfo setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
        return this;
    }

    public VaultSecretEngineInfo setRunningPluginVersion(String runningPluginVersion) {
        this.runningPluginVersion = runningPluginVersion;
        return this;
    }

    public VaultSecretEngineInfo setRunningSha256(String runningSha256) {
        this.runningSha256 = runningSha256;
        return this;
    }

    public VaultSecretEngineInfo setDefaultLeaseTimeToLive(Long defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public VaultSecretEngineInfo setMaxLeaseTimeToLive(Long maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }

    public VaultSecretEngineInfo setForceNoCache(Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }

    public VaultSecretEngineInfo setOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }

    public VaultSecretEngineInfo setAuditNonHMACRequestKeys(List<String> auditNonHMACRequestKeys) {
        this.auditNonHMACRequestKeys = auditNonHMACRequestKeys;
        return this;
    }

    public VaultSecretEngineInfo setAuditNonHMACResponseKeys(List<String> auditNonHMACResponseKeys) {
        this.auditNonHMACResponseKeys = auditNonHMACResponseKeys;
        return this;
    }

    public VaultSecretEngineInfo setListingVisibility(EngineListingVisibility listingVisibility) {
        this.listingVisibility = listingVisibility;
        return this;
    }

    public VaultSecretEngineInfo setPassthroughRequestHeaders(List<String> passthroughRequestHeaders) {
        this.passthroughRequestHeaders = passthroughRequestHeaders;
        return this;
    }

    public VaultSecretEngineInfo setAllowedResponseHeaders(List<String> allowedResponseHeaders) {
        this.allowedResponseHeaders = allowedResponseHeaders;
        return this;
    }

    public VaultSecretEngineInfo setAllowedManagedKeys(List<String> allowedManagedKeys) {
        this.allowedManagedKeys = allowedManagedKeys;
        return this;
    }
}

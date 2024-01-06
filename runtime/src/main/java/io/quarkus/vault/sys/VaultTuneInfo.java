package io.quarkus.vault.sys;

import java.util.List;
import java.util.Map;

public class VaultTuneInfo {

    private String description;
    private Long defaultLeaseTimeToLive;
    private Long maxLeaseTimeToLive;
    private Boolean forceNoCache;
    private Map<String, Object> options;
    private List<String> auditNonHMACRequestKeys;
    private List<String> auditNonHMACResponseKeys;
    private EngineListingVisibility listingVisibility;
    private List<String> passthroughRequestHeaders;
    private List<String> allowedResponseHeaders;
    private List<String> allowedManagedKeys;

    public String getDescription() {
        return description;
    }

    public VaultTuneInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public Long getDefaultLeaseTimeToLive() {
        return defaultLeaseTimeToLive;
    }

    public VaultTuneInfo setDefaultLeaseTimeToLive(Long defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public Long getMaxLeaseTimeToLive() {
        return maxLeaseTimeToLive;
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

    public VaultTuneInfo setMaxLeaseTimeToLive(Long maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }

    public Boolean getForceNoCache() {
        return forceNoCache;
    }

    public VaultTuneInfo setForceNoCache(Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }

    public VaultTuneInfo setOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }

    public VaultTuneInfo setAuditNonHMACRequestKeys(List<String> auditNonHMACRequestKeys) {
        this.auditNonHMACRequestKeys = auditNonHMACRequestKeys;
        return this;
    }

    public VaultTuneInfo setAuditNonHMACResponseKeys(List<String> auditNonHMACResponseKeys) {
        this.auditNonHMACResponseKeys = auditNonHMACResponseKeys;
        return this;
    }

    public VaultTuneInfo setListingVisibility(EngineListingVisibility listingVisibility) {
        this.listingVisibility = listingVisibility;
        return this;
    }

    public VaultTuneInfo setPassthroughRequestHeaders(List<String> passthroughRequestHeaders) {
        this.passthroughRequestHeaders = passthroughRequestHeaders;
        return this;
    }

    public VaultTuneInfo setAllowedResponseHeaders(List<String> allowedResponseHeaders) {
        this.allowedResponseHeaders = allowedResponseHeaders;
        return this;
    }

    public VaultTuneInfo setAllowedManagedKeys(List<String> allowedManagedKeys) {
        this.allowedManagedKeys = allowedManagedKeys;
        return this;
    }
}

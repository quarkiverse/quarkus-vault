package io.quarkus.vault.sys;

import java.util.List;
import java.util.Map;

/**
 * Options for enabling a new secret engine.
 */
public class EnableEngineOptions {

    /**
     * Default lease duration.
     */
    public String defaultLeaseTimeToLive;

    /**
     * Max lease duration.
     */
    public String maxLeaseTimeToLive;

    /**
     * Disable caching
     */
    public Boolean forceNoCache;

    /**
     * List of keys that will not be HMAC'd by audit devices in the request data object.
     */
    public List<String> auditNonHMACRequestKeys;

    /**
     * List of keys that will not be HMAC'd by audit devices in the response data object.
     */
    public List<String> auditNonHMACResponseKeys;

    /**
     * Specifies whether to show this mount in the UI-specific listing endpoint. Valid values are "unauth"
     * or "hidden". If not set, behaves like "hidden".
     */
    public EngineListingVisibility listingVisibility;

    /**
     * List of headers to allow and pass from the request to the plugin.
     */
    public List<String> passthroughRequestHeaders;

    /**
     * List of headers to allow, allowing a plugin to include them in the response.
     */
    public List<String> allowedResponseHeaders;

    /**
     * Specifies the semantic version of the plugin to use, e.g. "v1.0.0". If unspecified, the server
     * will select any matching unversioned plugin that may have been registered, the latest versioned
     * plugin registered, or a built-in plugin in that order of precendence.
     */
    public String pluginVersion;

    /**
     * List of managed key registry entry names that the mount in question is allowed to access.
     */
    public List<String> allowedManagedKeys;

    /**
     * Engine specific mount options
     */
    public Map<String, String> options;

    public EnableEngineOptions setDefaultLeaseTimeToLive(String defaultLeaseTimeToLive) {
        this.defaultLeaseTimeToLive = defaultLeaseTimeToLive;
        return this;
    }

    public EnableEngineOptions setMaxLeaseTimeToLive(String maxLeaseTimeToLive) {
        this.maxLeaseTimeToLive = maxLeaseTimeToLive;
        return this;
    }

    public EnableEngineOptions setForceNoCache(Boolean forceNoCache) {
        this.forceNoCache = forceNoCache;
        return this;
    }

    public EnableEngineOptions setAuditNonHMACRequestKeys(List<String> auditNonHMACRequestKeys) {
        this.auditNonHMACRequestKeys = auditNonHMACRequestKeys;
        return this;
    }

    public EnableEngineOptions setAuditNonHMACResponseKeys(List<String> auditNonHMACResponseKeys) {
        this.auditNonHMACResponseKeys = auditNonHMACResponseKeys;
        return this;
    }

    public EnableEngineOptions setListingVisibility(EngineListingVisibility listingVisibility) {
        this.listingVisibility = listingVisibility;
        return this;
    }

    public EnableEngineOptions setPassthroughRequestHeaders(List<String> passthroughRequestHeaders) {
        this.passthroughRequestHeaders = passthroughRequestHeaders;
        return this;
    }

    public EnableEngineOptions setAllowedResponseHeaders(List<String> allowedResponseHeaders) {
        this.allowedResponseHeaders = allowedResponseHeaders;
        return this;
    }

    public EnableEngineOptions setPluginVersion(String pluginVersion) {
        this.pluginVersion = pluginVersion;
        return this;
    }

    public EnableEngineOptions setAllowedManagedKeys(List<String> allowedManagedKeys) {
        this.allowedManagedKeys = allowedManagedKeys;
        return this;
    }

    public EnableEngineOptions setOptions(Map<String, String> options) {
        this.options = options;
        return this;
    }
}

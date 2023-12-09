package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultTuneBody {

    public String description;

    @JsonProperty("default_lease_ttl")
    public Long defaultLeaseTimeToLive;

    @JsonProperty("max_lease_ttl")
    public Long maxLeaseTimeToLive;

    @JsonProperty("audit_non_hmac_request_keys")
    public List<String> auditNonHMACRequestKeys;

    @JsonProperty("audit_non_hmac_response_keys")
    public List<String> auditNonHMACResponseKeys;

    @JsonProperty("listing_visibility")
    public String listingVisibility;

    @JsonProperty("passthrough_request_headers")
    public List<String> passthroughRequestHeaders;

    @JsonProperty("allowed_response_headers")
    public List<String> allowedResponseHeaders;

    @JsonProperty("allowed_managed_keys")
    public List<String> allowedManagedKeys;

    @JsonProperty("plugin_version")
    public String pluginVersion;

}

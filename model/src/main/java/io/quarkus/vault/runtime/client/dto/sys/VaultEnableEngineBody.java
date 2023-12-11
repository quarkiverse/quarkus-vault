package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultEnableEngineBody {

    public static class Config {

        @JsonProperty("default_lease_ttl")
        public String defaultLeaseTimeToLive;

        @JsonProperty("max_lease_ttl")
        public String maxLeaseTimeToLive;

        @JsonProperty("force_no_cache")
        public Boolean forceNoCache;

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

        @JsonProperty("plugin_version")
        public String pluginVersion;

        @JsonProperty("allowed_managed_keys")
        public List<String> allowedManagedKeys;

    }

    public String type;

    public String description = "";

    public Config config;

    public Map<String, String> options;

}

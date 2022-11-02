package io.quarkus.vault.runtime.client.dto.sys;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultTuneData implements VaultModel {

    public String description;

    @JsonProperty("default_lease_ttl")
    public Long defaultLeaseTimeToLive;

    @JsonProperty("max_lease_ttl")
    public Long maxLeaseTimeToLive;

    @JsonProperty("force_no_cache")
    public Boolean forceNoCache;

}

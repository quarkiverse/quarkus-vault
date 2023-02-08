package io.quarkus.vault.runtime.client.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AbstractVaultDTO<DATA, AUTH> implements VaultModel {

    @JsonProperty("request_id")
    public String requestId;
    @JsonProperty("lease_id")
    public String leaseId;
    public boolean renewable;
    @JsonProperty("lease_duration")
    public int leaseDurationSecs;
    public DATA data;
    @JsonProperty("wrap_info")
    public WrapInfo wrapInfo;
    public List<String> warnings;
    public AUTH auth;

}

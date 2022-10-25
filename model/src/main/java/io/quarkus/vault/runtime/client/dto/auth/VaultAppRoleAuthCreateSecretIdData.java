package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthCreateSecretIdData implements VaultModel {

    @JsonProperty(value = "metadata", defaultValue = "")
    public Map<String, String> metadata;

    @JsonProperty(value = "cidr_list", defaultValue = "[]")
    public List<String> cidrList;

    @JsonProperty(value = "token_bound_cidrs", defaultValue = "[]")
    public List<String> tokenBoundCidrs;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VaultAppRoleAuthCreateSecretIdData setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<String> getCidrList() {
        return cidrList;
    }

    public VaultAppRoleAuthCreateSecretIdData setCidrList(List<String> cidrList) {
        this.cidrList = cidrList;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleAuthCreateSecretIdData setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }
}

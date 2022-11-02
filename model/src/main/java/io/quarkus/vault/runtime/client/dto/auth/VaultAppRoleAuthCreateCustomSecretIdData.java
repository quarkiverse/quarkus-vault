package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthCreateCustomSecretIdData implements VaultModel {

    @JsonProperty(value = "secret_id", defaultValue = "", required = true)
    public String secretId;

    @JsonProperty(value = "metadata", defaultValue = "")
    public Map<String, String> metadata;

    @JsonProperty(value = "cidr_list", defaultValue = "[]")
    public List<String> cidrList;

    @JsonProperty(value = "token_bound_cidrs", defaultValue = "[]")
    public List<String> tokenBoundCidrs;

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VaultAppRoleAuthCreateCustomSecretIdData setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<String> getCidrList() {
        return cidrList;
    }

    public VaultAppRoleAuthCreateCustomSecretIdData setCidrList(List<String> cidrList) {
        this.cidrList = cidrList;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleAuthCreateCustomSecretIdData setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }

    public String getSecretId() {
        return secretId;
    }

    public VaultAppRoleAuthCreateCustomSecretIdData setSecretId(String secretId) {
        this.secretId = secretId;
        return this;
    }
}

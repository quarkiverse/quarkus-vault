package io.quarkus.vault.runtime.client.dto.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthSecretIdData implements VaultModel {

    @JsonProperty(value = "cidr_list", defaultValue = "[]")
    public List<String> cidrList;

    @JsonProperty("creation_time")
    public OffsetDateTime creationTime;

    @JsonProperty("expiration_time")
    public OffsetDateTime expirationTime;

    @JsonProperty("last_updated_time")
    public OffsetDateTime lastUpdatedTime;

    @JsonProperty(value = "metadata", defaultValue = "")
    public Map<String, String> metadata;

    @JsonProperty("secret_id_accessor")
    public String secretIdAccessor;

    @JsonProperty("secret_id_num_uses")
    public Integer secretIdNumUses;

    @JsonProperty("secret_id_ttl")
    public Integer secretIdTtl;

    @JsonProperty(value = "token_bound_cidrs", defaultValue = "[]")
    public List<String> tokenBoundCidrs;

    public List<String> getCidrList() {
        return cidrList;
    }

    public VaultAppRoleAuthSecretIdData setCidrList(List<String> cidrList) {
        this.cidrList = cidrList;
        return this;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public VaultAppRoleAuthSecretIdData setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public OffsetDateTime getExpirationTime() {
        return expirationTime;
    }

    public VaultAppRoleAuthSecretIdData setExpirationTime(OffsetDateTime expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    public OffsetDateTime getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public VaultAppRoleAuthSecretIdData setLastUpdatedTime(OffsetDateTime lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VaultAppRoleAuthSecretIdData setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getSecretIdAccessor() {
        return secretIdAccessor;
    }

    public VaultAppRoleAuthSecretIdData setSecretIdAccessor(String secretIdAccessor) {
        this.secretIdAccessor = secretIdAccessor;
        return this;
    }

    public Integer getSecretIdNumUses() {
        return secretIdNumUses;
    }

    public VaultAppRoleAuthSecretIdData setSecretIdNumUses(Integer secretIdNumUses) {
        this.secretIdNumUses = secretIdNumUses;
        return this;
    }

    public Integer getSecretIdTtl() {
        return secretIdTtl;
    }

    public VaultAppRoleAuthSecretIdData setSecretIdTtl(Integer secretIdTtl) {
        this.secretIdTtl = secretIdTtl;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleAuthSecretIdData setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }
}

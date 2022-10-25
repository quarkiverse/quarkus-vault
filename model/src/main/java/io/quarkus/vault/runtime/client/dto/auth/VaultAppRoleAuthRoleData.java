package io.quarkus.vault.runtime.client.dto.auth;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultAppRoleAuthRoleData implements VaultModel {

    @JsonProperty(value = "bind_secret_id", defaultValue = "true")
    public Boolean bindSecretId;

    @JsonProperty(value = "secret_id_bound_cidrs", defaultValue = "[]")
    public List<String> secretIdBoundCidrs;

    @JsonProperty(value = "secret_id_num_uses", defaultValue = "0")
    public Integer secretIdNumUses;

    @JsonProperty(value = "secret_id_ttl", defaultValue = "")
    public String secretIdTtl;

    @JsonProperty(value = "local_secret_ids", defaultValue = "false")
    public Boolean localSecretIds;

    @JsonProperty(value = "token_ttl", defaultValue = "0")
    public Integer tokenTtl;

    @JsonProperty(value = "token_max_ttl", defaultValue = "0")
    public Integer tokenMaxTtl;

    @JsonProperty(value = "token_policies", defaultValue = "[]")
    public List<String> tokenPolicies;

    @JsonProperty(value = "token_bound_cidrs", defaultValue = "[]")
    public List<String> tokenBoundCidrs;

    @JsonProperty(value = "token_explicit_max_ttl", defaultValue = "0")
    public Integer tokenExplicitMaxTtl;

    @JsonProperty(value = "token_no_default_policy", defaultValue = "false")
    public Boolean tokenNoDefaultPolicy;

    @JsonProperty(value = "token_num_uses", defaultValue = "0")
    public Integer tokenNumUses;

    @JsonProperty(value = "token_period", defaultValue = "0")
    public Integer tokenPeriod;

    @JsonProperty(value = "token_type", defaultValue = "")
    public String tokenType;

    public Boolean getBindSecretId() {
        return bindSecretId;
    }

    public VaultAppRoleAuthRoleData setBindSecretId(Boolean bindSecretId) {
        this.bindSecretId = bindSecretId;
        return this;
    }

    public List<String> getSecretIdBoundCidrs() {
        return secretIdBoundCidrs;
    }

    public VaultAppRoleAuthRoleData setSecretIdBoundCidrs(List<String> secretIdBoundCidrs) {
        this.secretIdBoundCidrs = secretIdBoundCidrs;
        return this;
    }

    public Integer getSecretIdNumUses() {
        return secretIdNumUses;
    }

    public VaultAppRoleAuthRoleData setSecretIdNumUses(Integer secretIdNumUses) {
        this.secretIdNumUses = secretIdNumUses;
        return this;
    }

    public String getSecretIdTtl() {
        return secretIdTtl;
    }

    public VaultAppRoleAuthRoleData setSecretIdTtl(String secretIdTtl) {
        this.secretIdTtl = secretIdTtl;
        return this;
    }

    public Boolean getLocalSecretIds() {
        return localSecretIds;
    }

    public VaultAppRoleAuthRoleData setLocalSecretIds(Boolean localSecretIds) {
        this.localSecretIds = localSecretIds;
        return this;
    }

    public Integer getTokenTtl() {
        return tokenTtl;
    }

    public VaultAppRoleAuthRoleData setTokenTtl(Integer tokenTtl) {
        this.tokenTtl = tokenTtl;
        return this;
    }

    public Integer getTokenMaxTtl() {
        return tokenMaxTtl;
    }

    public VaultAppRoleAuthRoleData setTokenMaxTtl(Integer tokenMaxTtl) {
        this.tokenMaxTtl = tokenMaxTtl;
        return this;
    }

    public List<String> getTokenPolicies() {
        return tokenPolicies;
    }

    public VaultAppRoleAuthRoleData setTokenPolicies(List<String> tokenPolicies) {
        this.tokenPolicies = tokenPolicies;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleAuthRoleData setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }

    public Integer getTokenExplicitMaxTtl() {
        return tokenExplicitMaxTtl;
    }

    public VaultAppRoleAuthRoleData setTokenExplicitMaxTtl(Integer tokenExplicitMaxTtl) {
        this.tokenExplicitMaxTtl = tokenExplicitMaxTtl;
        return this;
    }

    public Boolean getTokenNoDefaultPolicy() {
        return tokenNoDefaultPolicy;
    }

    public VaultAppRoleAuthRoleData setTokenNoDefaultPolicy(Boolean tokenNoDefaultPolicy) {
        this.tokenNoDefaultPolicy = tokenNoDefaultPolicy;
        return this;
    }

    public Integer getTokenNumUses() {
        return tokenNumUses;
    }

    public VaultAppRoleAuthRoleData setTokenNumUses(Integer tokenNumUses) {
        this.tokenNumUses = tokenNumUses;
        return this;
    }

    public Integer getTokenPeriod() {
        return tokenPeriod;
    }

    public VaultAppRoleAuthRoleData setTokenPeriod(Integer tokenPeriod) {
        this.tokenPeriod = tokenPeriod;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public VaultAppRoleAuthRoleData setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }
}

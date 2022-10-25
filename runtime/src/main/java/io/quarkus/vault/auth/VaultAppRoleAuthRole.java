package io.quarkus.vault.auth;

import java.util.List;

public class VaultAppRoleAuthRole {

    public Boolean bindSecretId;
    public List<String> secretIdBoundCidrs;
    public Integer secretIdNumUses;
    public String secretIdTtl;
    public Boolean localSecretIds;
    public Integer tokenTtl;
    public Integer tokenMaxTtl;
    public List<String> tokenPolicies;
    public List<String> tokenBoundCidrs;
    public Integer tokenExplicitMaxTtl;
    public Boolean tokenNoDefaultPolicy;
    public Integer tokenNumUses;
    public Integer tokenPeriod;
    public String tokenType;

    public Boolean getBindSecretId() {
        return bindSecretId;
    }

    public VaultAppRoleAuthRole setBindSecretId(Boolean bindSecretId) {
        this.bindSecretId = bindSecretId;
        return this;
    }

    public List<String> getSecretIdBoundCidrs() {
        return secretIdBoundCidrs;
    }

    public VaultAppRoleAuthRole setSecretIdBoundCidrs(List<String> secretIdBoundCidrs) {
        this.secretIdBoundCidrs = secretIdBoundCidrs;
        return this;
    }

    public Integer getSecretIdNumUses() {
        return secretIdNumUses;
    }

    public VaultAppRoleAuthRole setSecretIdNumUses(Integer secretIdNumUses) {
        this.secretIdNumUses = secretIdNumUses;
        return this;
    }

    public String getSecretIdTtl() {
        return secretIdTtl;
    }

    public VaultAppRoleAuthRole setSecretIdTtl(String secretIdTtl) {
        this.secretIdTtl = secretIdTtl;
        return this;
    }

    public Boolean getLocalSecretIds() {
        return localSecretIds;
    }

    public VaultAppRoleAuthRole setLocalSecretIds(Boolean localSecretIds) {
        this.localSecretIds = localSecretIds;
        return this;
    }

    public Integer getTokenTtl() {
        return tokenTtl;
    }

    public VaultAppRoleAuthRole setTokenTtl(Integer tokenTtl) {
        this.tokenTtl = tokenTtl;
        return this;
    }

    public Integer getTokenMaxTtl() {
        return tokenMaxTtl;
    }

    public VaultAppRoleAuthRole setTokenMaxTtl(Integer tokenMaxTtl) {
        this.tokenMaxTtl = tokenMaxTtl;
        return this;
    }

    public List<String> getTokenPolicies() {
        return tokenPolicies;
    }

    public VaultAppRoleAuthRole setTokenPolicies(List<String> tokenPolicies) {
        this.tokenPolicies = tokenPolicies;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleAuthRole setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }

    public Integer getTokenExplicitMaxTtl() {
        return tokenExplicitMaxTtl;
    }

    public VaultAppRoleAuthRole setTokenExplicitMaxTtl(Integer tokenExplicitMaxTtl) {
        this.tokenExplicitMaxTtl = tokenExplicitMaxTtl;
        return this;
    }

    public Boolean getTokenNoDefaultPolicy() {
        return tokenNoDefaultPolicy;
    }

    public VaultAppRoleAuthRole setTokenNoDefaultPolicy(Boolean tokenNoDefaultPolicy) {
        this.tokenNoDefaultPolicy = tokenNoDefaultPolicy;
        return this;
    }

    public Integer getTokenNumUses() {
        return tokenNumUses;
    }

    public VaultAppRoleAuthRole setTokenNumUses(Integer tokenNumUses) {
        this.tokenNumUses = tokenNumUses;
        return this;
    }

    public Integer getTokenPeriod() {
        return tokenPeriod;
    }

    public VaultAppRoleAuthRole setTokenPeriod(Integer tokenPeriod) {
        this.tokenPeriod = tokenPeriod;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public VaultAppRoleAuthRole setTokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    @Override
    public String toString() {
        return "VaultAppRoleAuthRole{" +
                "bindSecretId=" + bindSecretId +
                ", secretIdBoundCidrs=" + secretIdBoundCidrs +
                ", secretIdNumUses=" + secretIdNumUses +
                ", secretIdTtl='" + secretIdTtl + '\'' +
                ", localSecretIds=" + localSecretIds +
                ", tokenTtl=" + tokenTtl +
                ", tokenMaxTtl=" + tokenMaxTtl +
                ", tokenPolicies=" + tokenPolicies +
                ", tokenBoundCidrs=" + tokenBoundCidrs +
                ", tokenExplicitMaxTtl=" + tokenExplicitMaxTtl +
                ", tokenNoDefaultPolicy=" + tokenNoDefaultPolicy +
                ", tokenNumUses=" + tokenNumUses +
                ", tokenPeriod=" + tokenPeriod +
                ", tokenType='" + tokenType + '\'' +
                '}';
    }
}

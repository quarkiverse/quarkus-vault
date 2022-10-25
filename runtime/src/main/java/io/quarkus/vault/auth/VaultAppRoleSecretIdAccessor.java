/*
 * Copyright (c) 2022 by Bank Lombard Odier & Co Ltd, Geneva, Switzerland. This software is subject
 * to copyright protection under the laws of Switzerland and other countries. ALL RIGHTS RESERVED.
 *
 */

package io.quarkus.vault.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

public class VaultAppRoleSecretIdAccessor {

    public List<String> cidrList;
    public OffsetDateTime creationTime;
    public OffsetDateTime expirationTime;
    public OffsetDateTime lastUpdatedTime;
    public Map<String, String> metadata;
    public String secretIdAccessor;
    public Integer secretIdNumUses;
    public Integer secretIdTtl;
    public List<String> tokenBoundCidrs;

    public List<String> getCidrList() {
        return cidrList;
    }

    public VaultAppRoleSecretIdAccessor setCidrList(List<String> cidrList) {
        this.cidrList = cidrList;
        return this;
    }

    public OffsetDateTime getCreationTime() {
        return creationTime;
    }

    public VaultAppRoleSecretIdAccessor setCreationTime(OffsetDateTime creationTime) {
        this.creationTime = creationTime;
        return this;
    }

    public OffsetDateTime getExpirationTime() {
        return expirationTime;
    }

    public VaultAppRoleSecretIdAccessor setExpirationTime(OffsetDateTime expirationTime) {
        this.expirationTime = expirationTime;
        return this;
    }

    public OffsetDateTime getLastUpdatedTime() {
        return lastUpdatedTime;
    }

    public VaultAppRoleSecretIdAccessor setLastUpdatedTime(OffsetDateTime lastUpdatedTime) {
        this.lastUpdatedTime = lastUpdatedTime;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VaultAppRoleSecretIdAccessor setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public String getSecretIdAccessor() {
        return secretIdAccessor;
    }

    public VaultAppRoleSecretIdAccessor setSecretIdAccessor(String secretIdAccessor) {
        this.secretIdAccessor = secretIdAccessor;
        return this;
    }

    public Integer getSecretIdNumUses() {
        return secretIdNumUses;
    }

    public VaultAppRoleSecretIdAccessor setSecretIdNumUses(Integer secretIdNumUses) {
        this.secretIdNumUses = secretIdNumUses;
        return this;
    }

    public Integer getSecretIdTtl() {
        return secretIdTtl;
    }

    public VaultAppRoleSecretIdAccessor setSecretIdTtl(Integer secretIdTtl) {
        this.secretIdTtl = secretIdTtl;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleSecretIdAccessor setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }
}

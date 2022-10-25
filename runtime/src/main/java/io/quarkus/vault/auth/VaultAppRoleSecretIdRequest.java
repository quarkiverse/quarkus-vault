/*
 * Copyright (c) 2022 by Bank Lombard Odier & Co Ltd, Geneva, Switzerland. This software is subject
 * to copyright protection under the laws of Switzerland and other countries. ALL RIGHTS RESERVED.
 *
 */

package io.quarkus.vault.auth;

import java.util.List;
import java.util.Map;

public class VaultAppRoleSecretIdRequest {

    public String secretId;
    public Map<String, String> metadata;
    public List<String> cidrList;
    public List<String> tokenBoundCidrs;

    public String getSecretId() {
        return secretId;
    }

    public VaultAppRoleSecretIdRequest setSecretId(String secretId) {
        this.secretId = secretId;
        return this;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public VaultAppRoleSecretIdRequest setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
        return this;
    }

    public List<String> getCidrList() {
        return cidrList;
    }

    public VaultAppRoleSecretIdRequest setCidrList(List<String> cidrList) {
        this.cidrList = cidrList;
        return this;
    }

    public List<String> getTokenBoundCidrs() {
        return tokenBoundCidrs;
    }

    public VaultAppRoleSecretIdRequest setTokenBoundCidrs(List<String> tokenBoundCidrs) {
        this.tokenBoundCidrs = tokenBoundCidrs;
        return this;
    }
}

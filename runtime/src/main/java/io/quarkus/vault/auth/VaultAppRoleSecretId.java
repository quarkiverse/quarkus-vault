/*
 * Copyright (c) 2022 by Bank Lombard Odier & Co Ltd, Geneva, Switzerland. This software is subject
 * to copyright protection under the laws of Switzerland and other countries. ALL RIGHTS RESERVED.
 *
 */

package io.quarkus.vault.auth;

public class VaultAppRoleSecretId {

    public String secretId;
    public String secretIdAccessor;

    public String getSecretId() {
        return secretId;
    }

    public VaultAppRoleSecretId setSecretId(String secretId) {
        this.secretId = secretId;
        return this;
    }

    public String getSecretIdAccessor() {
        return secretIdAccessor;
    }

    public VaultAppRoleSecretId setSecretIdAccessor(String secretIdAccessor) {
        this.secretIdAccessor = secretIdAccessor;
        return this;
    }
}

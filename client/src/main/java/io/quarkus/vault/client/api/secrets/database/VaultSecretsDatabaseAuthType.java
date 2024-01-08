package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

public enum VaultSecretsDatabaseAuthType implements VaultModel {

    @JsonProperty("gcp_iam")
    GCP_IAM("gcp_iam");

    private final String value;

    VaultSecretsDatabaseAuthType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @JsonCreator
    public static VaultSecretsDatabaseAuthType from(String value) {
        if (value == null)
            return null;
        for (var v : values()) {
            if (v.value.equals(value))
                return v;
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}

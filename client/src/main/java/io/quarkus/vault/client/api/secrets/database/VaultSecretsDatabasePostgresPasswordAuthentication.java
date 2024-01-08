package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

public enum VaultSecretsDatabasePostgresPasswordAuthentication implements VaultModel {

    @JsonProperty("scram-sha-256")
    SCRAM_SHA_256("scram-sha-256"),

    @JsonProperty("password")
    PASSWORD("password");

    private final String value;

    VaultSecretsDatabasePostgresPasswordAuthentication(String value) {
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
    public static VaultSecretsDatabasePostgresPasswordAuthentication from(String value) {
        if (value == null)
            return null;
        for (var v : values()) {
            if (v.value.equals(value))
                return v;
        }
        throw new IllegalArgumentException("Unknown value: " + value);
    }
}

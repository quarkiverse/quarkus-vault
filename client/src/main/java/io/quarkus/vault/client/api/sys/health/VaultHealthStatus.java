package io.quarkus.vault.client.api.sys.health;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

public enum VaultHealthStatus implements VaultModel {
    INITIALIZED_UNSEALED_ACTIVE(200),
    UNSEALED_STANDBY(429),
    RECOVERY_REPLICATION_SECONDARY_ACTIVE(472),
    PERFORMANCE_STANDBY(473),
    NOT_INITIALIZED(501),
    SEALED(503);

    private final int statusCode;

    VaultHealthStatus(int statusCode) {
        this.statusCode = statusCode;
    }

    @JsonValue
    public int getStatusCode() {
        return statusCode;
    }

    @JsonCreator
    public static VaultHealthStatus fromStatusCode(int statusCode) {
        for (VaultHealthStatus status : VaultHealthStatus.values()) {
            if (status.getStatusCode() == statusCode) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown Vault health status code: " + statusCode);
    }
}

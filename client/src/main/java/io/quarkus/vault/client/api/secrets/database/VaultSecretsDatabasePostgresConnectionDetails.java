package io.quarkus.vault.client.api.secrets.database;

import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecretsDatabasePostgresConnectionDetails extends VaultSecretsDatabaseConnectionDetails {

    @JsonProperty("max_open_connections")
    private Integer maxOpenConnections;

    @JsonProperty("max_idle_connections")
    private Integer maxIdleConnections;

    @JsonProperty("max_connection_lifetime")
    private Duration maxConnectionLifetime;

    @JsonProperty("auth_type")
    private VaultSecretsDatabaseAuthType authType;

    @JsonProperty("service_account_json")
    private String serviceAccount;

    @JsonProperty("username_template")
    private String usernameTemplate;

    @JsonProperty("disable_escaping")
    private Boolean disableEscaping;

    @JsonProperty("password_authentication")
    private VaultSecretsDatabasePostgresPasswordAuthentication passwordAuthentication;

    public Integer getMaxOpenConnections() {
        return maxOpenConnections;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setMaxOpenConnections(Integer maxOpenConnections) {
        this.maxOpenConnections = maxOpenConnections;
        return this;
    }

    public Integer getMaxIdleConnections() {
        return maxIdleConnections;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setMaxIdleConnections(Integer maxIdleConnections) {
        this.maxIdleConnections = maxIdleConnections;
        return this;
    }

    public Duration getMaxConnectionLifetime() {
        return maxConnectionLifetime;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setMaxConnectionLifetime(Duration maxConnectionLifetime) {
        this.maxConnectionLifetime = maxConnectionLifetime;
        return this;
    }

    public VaultSecretsDatabaseAuthType getAuthType() {
        return authType;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setAuthType(VaultSecretsDatabaseAuthType authType) {
        this.authType = authType;
        return this;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
        return this;
    }

    public String getUsernameTemplate() {
        return usernameTemplate;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setUsernameTemplate(String usernameTemplate) {
        this.usernameTemplate = usernameTemplate;
        return this;
    }

    public Boolean isDisableEscaping() {
        return disableEscaping;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setDisableEscaping(Boolean disableEscaping) {
        this.disableEscaping = disableEscaping;
        return this;
    }

    public VaultSecretsDatabasePostgresPasswordAuthentication getPasswordAuthentication() {
        return passwordAuthentication;
    }

    public VaultSecretsDatabasePostgresConnectionDetails setPasswordAuthentication(
            VaultSecretsDatabasePostgresPasswordAuthentication passwordAuthentication) {
        this.passwordAuthentication = passwordAuthentication;
        return this;
    }
}

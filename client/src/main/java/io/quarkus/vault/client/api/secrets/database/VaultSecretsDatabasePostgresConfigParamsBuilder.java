package io.quarkus.vault.client.api.secrets.database;

import static io.quarkus.vault.client.json.JsonMapping.convert;

import java.time.Duration;
import java.util.List;

public class VaultSecretsDatabasePostgresConfigParamsBuilder {

    private final VaultSecretsDatabaseConfigParams params;

    public VaultSecretsDatabasePostgresConfigParamsBuilder() {
        this.params = new VaultSecretsDatabaseConfigParams();
        this.params.setPluginName("postgresql-database-plugin");
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder connectionUrl(String connectionUrl) {
        this.params.addProperty("connection_url", connectionUrl);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder maxOpenConnections(Integer maxOpenConnections) {
        this.params.addProperty("max_open_connections", maxOpenConnections);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder maxIdleConnections(Integer maxIdleConnections) {
        this.params.addProperty("max_idle_connections", maxIdleConnections);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder maxConnectionLifetime(Duration maxConnectionLifetime) {
        this.params.addProperty("max_connection_lifetime", maxConnectionLifetime);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder username(String username) {
        this.params.addProperty("username", username);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder password(String password) {
        this.params.addProperty("password", password);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder authType(VaultSecretsDatabaseAuthType authType) {
        this.params.addProperty("auth_type", authType);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder serviceAccount(String serviceAccount) {
        this.params.addProperty("service_account_json", serviceAccount);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder usernameTemplate(String usernameTemplate) {
        this.params.addProperty("username_template", usernameTemplate);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder disableEscaping(Boolean disableEscaping) {
        this.params.addProperty("disable_escaping", disableEscaping);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder passwordAuthentication(
            VaultSecretsDatabasePostgresPasswordAuthentication passwordAuthentication) {
        this.params.addProperty("password_authentication", convert(passwordAuthentication, String.class));
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder verifyConnection(Boolean verifyConnection) {
        this.params.setVerifyConnection(verifyConnection);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder allowedRoles(List<String> allowedRoles) {
        this.params.setAllowedRoles(allowedRoles);
        return this;
    }

    public VaultSecretsDatabasePostgresConfigParamsBuilder pluginVersion(String pluginVersion) {
        this.params.setPluginVersion(pluginVersion);
        return this;
    }

    public VaultSecretsDatabaseConfigParams build() {
        return this.params;
    }

}

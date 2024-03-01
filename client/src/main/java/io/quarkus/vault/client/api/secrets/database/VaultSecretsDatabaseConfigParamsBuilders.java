package io.quarkus.vault.client.api.secrets.database;

public interface VaultSecretsDatabaseConfigParamsBuilders {

    static VaultSecretsDatabasePostgresConfigParamsBuilder postgresBuilder() {
        return new VaultSecretsDatabasePostgresConfigParamsBuilder();
    }

}

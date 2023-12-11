package io.quarkus.vault.sys;

import java.util.List;

public class VaultPlugins {

    private List<String> auth;
    private List<String> database;
    private List<String> secret;
    private List<VaultPluginDetails> detailed;

    public List<String> getAuth() {
        return auth;
    }

    public VaultPlugins setAuth(List<String> auth) {
        this.auth = auth;
        return this;
    }

    public List<String> getDatabase() {
        return database;
    }

    public VaultPlugins setDatabase(List<String> database) {
        this.database = database;
        return this;
    }

    public List<String> getSecret() {
        return secret;
    }

    public VaultPlugins setSecret(List<String> secret) {
        this.secret = secret;
        return this;
    }

    public List<VaultPluginDetails> getDetailed() {
        return detailed;
    }

    public VaultPlugins setDetailed(List<VaultPluginDetails> detailed) {
        this.detailed = detailed;
        return this;
    }
}

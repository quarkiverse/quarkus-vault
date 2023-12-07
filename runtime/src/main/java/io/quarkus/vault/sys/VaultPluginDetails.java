package io.quarkus.vault.sys;

import java.util.List;

public class VaultPluginDetails {

    private Boolean builtin;

    private String deprecationStatus;

    private String name;

    private String type;

    private String version;

    private String sha256;

    private String command;

    private List<String> args;

    private List<String> env;

    public Boolean getBuiltin() {
        return builtin;
    }

    public VaultPluginDetails setBuiltin(Boolean builtin) {
        this.builtin = builtin;
        return this;
    }

    public String getDeprecationStatus() {
        return deprecationStatus;
    }

    public VaultPluginDetails setDeprecationStatus(String deprecationStatus) {
        this.deprecationStatus = deprecationStatus;
        return this;
    }

    public String getName() {
        return name;
    }

    public VaultPluginDetails setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public VaultPluginDetails setType(String type) {
        this.type = type;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public VaultPluginDetails setVersion(String version) {
        this.version = version;
        return this;
    }

    public String getSha256() {
        return sha256;
    }

    public VaultPluginDetails setSha256(String sha256) {
        this.sha256 = sha256;
        return this;
    }

    public String getCommand() {
        return command;
    }

    public VaultPluginDetails setCommand(String command) {
        this.command = command;
        return this;
    }

    public List<String> getArgs() {
        return args;
    }

    public VaultPluginDetails setArgs(List<String> args) {
        this.args = args;
        return this;
    }

    public List<String> getEnv() {
        return env;
    }

    public VaultPluginDetails setEnv(List<String> env) {
        this.env = env;
        return this;
    }
}

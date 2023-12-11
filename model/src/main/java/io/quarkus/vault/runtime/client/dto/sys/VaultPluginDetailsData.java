package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

import jakarta.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultPluginDetailsData {

    public Boolean builtin;

    @JsonProperty("deprecation_status")
    @Nullable
    public String deprecationStatus;

    public String name;

    public String type;

    @Nullable
    public String version;

    @Nullable
    public String sha256;

    @Nullable
    public String command;

    @Nullable
    public List<String> args;

    @Nullable
    public List<String> env;
}

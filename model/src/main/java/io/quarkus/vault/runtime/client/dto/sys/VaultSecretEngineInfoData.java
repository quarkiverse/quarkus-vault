package io.quarkus.vault.runtime.client.dto.sys;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.runtime.client.dto.VaultModel;

public class VaultSecretEngineInfoData implements VaultModel {

    public VaultSecretEngineConfigData config;

    public String description;

    @JsonProperty("external_entropy_access")
    public Boolean externalEntropyAccess;

    public Boolean local;

    @JsonProperty("seal_wrap")
    public Boolean sealWrap;

    public String type;

    @JsonProperty("plugin_version")
    public String pluginVersion;

    @JsonProperty("running_plugin_version")
    public String runningPluginVersion;

    @JsonProperty("running_sha256")
    public String runningSha256;

    public Map<String, Object> options;
}

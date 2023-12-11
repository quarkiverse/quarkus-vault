package io.quarkus.vault.runtime.client.dto.sys;

import java.util.List;

public class VaultListAllPluginsData {

    public List<String> auth;

    public List<String> secret;

    public List<String> database;
    public List<VaultPluginDetailsData> detailed;

}

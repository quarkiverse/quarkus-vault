package io.quarkus.vault.runtime.client.dto.sys;

import java.util.Map;

public class VaultTuneData extends VaultSecretEngineConfigData {

    public String description;

    public Map<String, String> options;

}

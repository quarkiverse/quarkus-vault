package io.quarkus.vault.sys;

import java.util.Map;

public class VaultSecretEngineInfo {

    private String description;

    private Boolean externalEntropyAccess;

    private Boolean local;

    private Boolean sealWrap;

    private String type;

    private Map<String, Object> options;

    public Boolean getExternalEntropyAccess() {
        return externalEntropyAccess;
    }

    public Boolean getLocal() {
        return local;
    }

    public Boolean getSealWrap() {
        return sealWrap;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public String getType() {
        return type;
    }

    public VaultSecretEngineInfo setDescription(String description) {
        this.description = description;
        return this;
    }

    public VaultSecretEngineInfo setExternalEntropyAccess(Boolean externalEntropyAccess) {
        this.externalEntropyAccess = externalEntropyAccess;
        return this;
    }

    public VaultSecretEngineInfo setLocal(Boolean local) {
        this.local = local;
        return this;
    }

    public VaultSecretEngineInfo setSealWrap(Boolean sealWrap) {
        this.sealWrap = sealWrap;
        return this;
    }

    public VaultSecretEngineInfo setType(String type) {
        this.type = type;
        return this;
    }

    public VaultSecretEngineInfo setOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }
}

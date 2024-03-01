package io.quarkus.vault.client.api.common;

public abstract class VaultRequestFactory {

    private final String traceNameTag;

    protected VaultRequestFactory(String traceNameTag) {
        this.traceNameTag = traceNameTag;
    }

    protected String getTraceOpName(String suffix) {
        return "VAULT " + traceNameTag + " " + suffix;
    }

}

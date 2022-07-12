package io.quarkus.vault.runtime.client;

public abstract class VaultInternalBase {

    protected String opNamePrefix() {
        return "VAULT";
    }

    protected String opName(String name) {
        return opNamePrefix() + " " + name;
    }

}

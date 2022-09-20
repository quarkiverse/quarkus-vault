package io.quarkus.vault.runtime.config;

import java.util.Map;

import io.quarkus.runtime.annotations.ConfigGroup;

@ConfigGroup
public interface VaultTransitConfig {

    /**
     * keys
     */
    Map<String, TransitKeyConfig> key();

}

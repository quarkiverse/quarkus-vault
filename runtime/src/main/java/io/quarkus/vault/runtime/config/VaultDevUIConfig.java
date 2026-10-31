package io.quarkus.vault.runtime.config;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.smallrye.config.WithDefault;

@ConfigGroup
public interface VaultDevUIConfig {
    /**
     * Expose config source metadata to connected Dev MCP clients in dev mode.
     * This includes KV paths, property names, fetch times and refresh errors, but never secret values.
     * The Dev UI Config sources page remains available regardless of this setting.
     * Enable only when you intend to share this metadata with connected MCP clients.
     * Ignored outside dev mode.
     */
    @WithDefault("false")
    boolean exposeConfigSourceMetadataToMcp();
}

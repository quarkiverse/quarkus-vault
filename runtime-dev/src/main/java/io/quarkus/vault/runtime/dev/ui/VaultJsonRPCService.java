package io.quarkus.vault.runtime.dev.ui;

import java.net.URL;
import java.util.Map;
import java.util.Optional;

import jakarta.inject.Inject;

import org.jboss.logging.Logger;

import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.runtime.config.VaultConfigSourceStatus;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultJsonRPCService {

    private static final Logger log = Logger.getLogger(VaultJsonRPCService.class);

    private static final String UNREACHABLE = "unreachable";
    private static final String ERROR = "error";
    private static final String MCP_EXPOSURE_PROPERTY = "quarkus.vault.dev-ui.expose-config-source-metadata-to-mcp";

    @Inject
    VaultSystemBackendEngine systemBackendEngine;

    @Inject
    VaultRuntimeConfig runtimeConfig;

    public String getServerVersion() {
        if (runtimeConfig.url().isEmpty()) {
            return null;
        }
        try {
            return systemBackendEngine.sealStatus().getVersion();
        } catch (RuntimeException e) {
            log.debug("unable to read the Vault server version", e);
            return UNREACHABLE.equals(VaultConfigSourceStatus.errorCategory(e)) ? UNREACHABLE : ERROR;
        }
    }

    public String getVaultUiUrl() {
        return runtimeConfig.url()
                .map(URL::toString)
                .map(url -> url.replaceAll("/+$", "") + "/ui")
                .orElse(null);
    }

    public VaultConfigSourceStatus.Snapshot getConfigSources() {
        return VaultConfigSourceStatus.last().orElse(null);
    }

    @JsonRpcDescription("Read-only: property names loaded by the Vault config source per KV path, with fetch time and last error. Never values. Requires "
            + MCP_EXPOSURE_PROPERTY + "=true.")
    @DevMCPEnableByDefault
    public Object getConfigSourcesForMcp() {
        if (LaunchMode.current() != LaunchMode.DEVELOPMENT || !runtimeConfig.devUi().exposeConfigSourceMetadataToMcp()) {
            return Map.of("message", "Vault config source metadata is not shared with MCP clients. Set "
                    + MCP_EXPOSURE_PROPERTY + "=true in the application configuration to share it.");
        }
        Optional<VaultConfigSourceStatus.Snapshot> snapshot = VaultConfigSourceStatus.last();
        if (snapshot.isEmpty()) {
            return Map.of("message", "The Vault config source has not fetched any KV path yet.");
        }
        return snapshot.get();
    }

    public String getConfigSourcesLabel() {
        Optional<VaultConfigSourceStatus.Snapshot> snapshot = VaultConfigSourceStatus.last();
        if (snapshot.isEmpty() || snapshot.get().paths().isEmpty()) {
            return null;
        }
        var paths = snapshot.get().paths();
        if (snapshot.get().refreshError() != null || paths.stream().anyMatch(path -> path.error() != null)) {
            return ERROR;
        }
        return String.valueOf(paths.stream().mapToInt(path -> path.propertyNames().size()).sum());
    }
}

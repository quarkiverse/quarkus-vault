package io.quarkus.vault.runtime.dev.ui;

import java.net.URL;
import java.util.Optional;

import jakarta.inject.Inject;

import io.quarkus.runtime.annotations.DevMCPEnableByDefault;
import io.quarkus.runtime.annotations.JsonRpcDescription;
import io.quarkus.vault.VaultSystemBackendEngine;
import io.quarkus.vault.runtime.config.VaultConfigSourceStatus;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;

public class VaultJsonRPCService {

    private static final String UNREACHABLE = "unreachable";
    private static final String ERROR = "error";

    @Inject
    VaultSystemBackendEngine systemBackendEngine;

    @Inject
    VaultRuntimeConfig runtimeConfig;

    public String getServerVersion() {
        try {
            return systemBackendEngine.sealStatus().getVersion();
        } catch (RuntimeException e) {
            return UNREACHABLE;
        }
    }

    public String getVaultUiUrl() {
        return runtimeConfig.url()
                .map(URL::toString)
                .map(url -> url.replaceAll("/+$", "") + "/ui")
                .orElse(null);
    }

    @JsonRpcDescription("Read-only: property names loaded by the Vault config source per KV path, with fetch time and last error. Never values.")
    @DevMCPEnableByDefault
    public VaultConfigSourceStatus.Snapshot getConfigSources() {
        return VaultConfigSourceStatus.last().orElse(null);
    }

    public String getConfigSourcesLabel() {
        Optional<VaultConfigSourceStatus.Snapshot> snapshot = VaultConfigSourceStatus.last();
        if (snapshot.isEmpty() || snapshot.get().paths().isEmpty()) {
            return null;
        }
        var paths = snapshot.get().paths();
        if (paths.stream().anyMatch(path -> path.error() != null)) {
            return ERROR;
        }
        return String.valueOf(paths.stream().mapToInt(path -> path.propertyNames().size()).sum());
    }
}

package io.quarkus.vault.runtime;

import java.util.List;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultSystemBackendReactiveEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultEnableEngineBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultRegisterPluginBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneBody;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.EngineListingVisibility;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultPluginDetails;
import io.quarkus.vault.sys.VaultPlugins;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultSystemBackendManager implements VaultSystemBackendReactiveEngine {
    @Inject
    VaultClient vaultClient;
    @Inject
    VaultBuildTimeConfig buildTimeConfig;
    @Inject
    VaultAuthManager vaultAuthManager;
    @Inject
    VaultInternalSystemBackend vaultInternalSystemBackend;

    @Override
    public Uni<VaultInit> init(int secretShares, int secretThreshold) {
        return vaultInternalSystemBackend.init(vaultClient, secretShares, secretThreshold)
                .map(init -> new VaultInit(init.keys, init.keysBase64, init.rootToken));
    }

    @Override
    public Uni<VaultHealth> health() {
        boolean isStandByOk = this.buildTimeConfig.health().standByOk();
        boolean isPerfStandByOk = this.buildTimeConfig.health().performanceStandByOk();

        return this.health(isStandByOk, isPerfStandByOk);
    }

    @Override
    public Uni<VaultHealthStatus> healthStatus() {
        boolean isStandByOk = this.buildTimeConfig.health().standByOk();
        boolean isPerfStandByOk = this.buildTimeConfig.health().performanceStandByOk();

        return this.healthStatus(isStandByOk, isPerfStandByOk);
    }

    @Override
    public Uni<VaultSealStatus> sealStatus() {
        return vaultInternalSystemBackend.systemSealStatus(vaultClient)
                .map(vaultSealStatusResult -> {

                    final VaultSealStatus vaultSealStatus = new VaultSealStatus();
                    vaultSealStatus.setClusterId(vaultSealStatusResult.clusterId);
                    vaultSealStatus.setClusterName(vaultSealStatusResult.clusterName);
                    vaultSealStatus.setInitialized(vaultSealStatusResult.initialized);
                    vaultSealStatus.setMigration(vaultSealStatusResult.migration);
                    vaultSealStatus.setN(vaultSealStatusResult.n);
                    vaultSealStatus.setNonce(vaultSealStatusResult.nonce);
                    vaultSealStatus.setProgress(vaultSealStatusResult.progress);
                    vaultSealStatus.setRecoverySeal(vaultSealStatusResult.recoverySeal);
                    vaultSealStatus.setSealed(vaultSealStatusResult.sealedStatus);
                    vaultSealStatus.setT(vaultSealStatusResult.t);
                    vaultSealStatus.setType(vaultSealStatusResult.type);
                    vaultSealStatus.setVersion(vaultSealStatusResult.version);

                    return vaultSealStatus;
                });
    }

    private Uni<VaultHealthStatus> healthStatus(boolean isStandByOk, boolean isPerfStandByOk) {
        return vaultInternalSystemBackend.systemHealthStatus(vaultClient, isStandByOk, isPerfStandByOk)
                .map(vaultHealthResult -> {

                    final VaultHealthStatus vaultHealthStatus = new VaultHealthStatus();
                    vaultHealthStatus.setClusterId(vaultHealthResult.clusterId);
                    vaultHealthStatus.setClusterName(vaultHealthResult.clusterName);
                    vaultHealthStatus.setInitialized(vaultHealthResult.initialized);
                    vaultHealthStatus.setPerformanceStandby(vaultHealthResult.performanceStandby);
                    vaultHealthStatus.setReplicationDrMode(vaultHealthResult.replicationDrMode);
                    vaultHealthStatus.setReplicationPerfMode(vaultHealthResult.replicationPerfMode);
                    vaultHealthStatus.setSealed(vaultHealthResult.sealedStatus);
                    vaultHealthStatus.setServerTimeUtc(vaultHealthResult.serverTimeUtc);
                    vaultHealthStatus.setStandby(vaultHealthResult.standby);
                    vaultHealthStatus.setVersion(vaultHealthResult.version);

                    return vaultHealthStatus;
                });
    }

    private Uni<VaultHealth> health(boolean isStandByOk, boolean isPerfStandByOk) {
        return vaultInternalSystemBackend.systemHealth(vaultClient, isStandByOk, isPerfStandByOk)
                .map(VaultHealth::new);
    }

    @Override
    public Uni<String> getPolicyRules(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.getPolicy(vaultClient, token, name).map(r -> r.data.rules);
        });
    }

    @Override
    public Uni<Void> createUpdatePolicy(String name, String policy) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.createUpdatePolicy(vaultClient, token, name, new VaultPolicyBody(policy));
        });
    }

    @Override
    public Uni<Void> deletePolicy(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.deletePolicy(vaultClient, token, name);
        });
    }

    @Override
    public Uni<List<String>> getPolicies() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.listPolicies(vaultClient, token).map(r -> r.data.policies);
        });
    }

    @Override
    public Uni<VaultSecretEngineInfo> getSecretEngineInfo(String mount) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.getSecretEngineInfo(vaultClient, token, mount)
                    .map(result -> {
                        VaultSecretEngineInfo info = new VaultSecretEngineInfo();
                        info.setDescription(result.data.description);
                        info.setType(result.data.type);
                        info.setLocal(result.data.local);
                        info.setExternalEntropyAccess(result.data.externalEntropyAccess);
                        info.setSealWrap(result.data.sealWrap);
                        info.setPluginVersion(result.data.pluginVersion);
                        info.setRunningPluginVersion(result.data.runningPluginVersion);
                        info.setRunningSha256(result.data.runningSha256);
                        info.setDefaultLeaseTimeToLive(result.data.config.defaultLeaseTimeToLive);
                        info.setMaxLeaseTimeToLive(result.data.config.maxLeaseTimeToLive);
                        info.setForceNoCache(result.data.config.forceNoCache);
                        info.setOptions(result.data.options);
                        info.setAuditNonHMACRequestKeys(result.data.config.auditNonHMACRequestKeys);
                        info.setAuditNonHMACResponseKeys(result.data.config.auditNonHMACResponseKeys);
                        info.setListingVisibility(result.data.config.listingVisibility != null
                                ? EngineListingVisibility.valueOf(result.data.config.listingVisibility
                                        .toUpperCase())
                                : null);
                        info.setPassthroughRequestHeaders(result.data.config.passthroughRequestHeaders);
                        info.setAllowedResponseHeaders(result.data.config.allowedResponseHeaders);
                        info.setAllowedManagedKeys(result.data.config.allowedManagedKeys);
                        return info;
                    });
        });
    }

    public Uni<VaultTuneInfo> getTuneInfo(String mount) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.getTuneInfo(vaultClient, token, mount)
                    .map(vaultTuneResult -> {
                        VaultTuneInfo tuneInfo = new VaultTuneInfo();
                        tuneInfo.setDefaultLeaseTimeToLive(vaultTuneResult.data.defaultLeaseTimeToLive);
                        tuneInfo.setMaxLeaseTimeToLive(vaultTuneResult.data.maxLeaseTimeToLive);
                        tuneInfo.setDescription(vaultTuneResult.data.description);
                        tuneInfo.setForceNoCache(vaultTuneResult.data.forceNoCache);
                        tuneInfo.setOptions(vaultTuneResult.data.options);
                        tuneInfo.setAuditNonHMACRequestKeys(vaultTuneResult.data.auditNonHMACRequestKeys);
                        tuneInfo.setAuditNonHMACResponseKeys(vaultTuneResult.data.auditNonHMACResponseKeys);
                        tuneInfo.setListingVisibility(vaultTuneResult.data.listingVisibility != null
                                ? EngineListingVisibility.valueOf(vaultTuneResult.data.listingVisibility
                                        .toUpperCase())
                                : null);
                        tuneInfo.setPassthroughRequestHeaders(vaultTuneResult.data.passthroughRequestHeaders);
                        tuneInfo.setAllowedResponseHeaders(vaultTuneResult.data.allowedResponseHeaders);
                        tuneInfo.setAllowedManagedKeys(vaultTuneResult.data.allowedManagedKeys);
                        return tuneInfo;
                    });
        });
    }

    @Override
    public Uni<Void> updateTuneInfo(String mount, VaultTuneInfo tuneInfoUpdates) {
        VaultTuneBody body = new VaultTuneBody();
        body.description = tuneInfoUpdates.getDescription();
        body.defaultLeaseTimeToLive = tuneInfoUpdates.getDefaultLeaseTimeToLive();
        body.maxLeaseTimeToLive = tuneInfoUpdates.getMaxLeaseTimeToLive();
        body.forceNoCache = tuneInfoUpdates.getForceNoCache();

        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.updateTuneInfo(vaultClient, token, mount, body);
        });
    }

    @Override
    public Uni<Boolean> isEngineMounted(String mount) {
        return getSecretEngineInfo(mount).map(i -> true)
                .onFailure(VaultClientException.class).recoverWithUni(x -> {
                    if (((VaultClientException) x).getStatus() == 405) {
                        // Fallback for < 1.10.0
                        return getTuneInfo(mount).map(i -> true);
                    }
                    return Uni.createFrom().failure(x);
                })
                .onFailure(VaultClientException.class).recoverWithUni(x -> {
                    if (((VaultClientException) x).getStatus() == 400) {
                        return Uni.createFrom().item(false);
                    }
                    return Uni.createFrom().failure(x);
                });
    }

    public Uni<Void> enable(VaultSecretEngine engine, String mount, String description, EnableEngineOptions options) {
        return enable(engine.getType(), mount, description, options);
    }

    public Uni<Void> enable(String engineType, String mount, String description, EnableEngineOptions options) {
        VaultEnableEngineBody body = new VaultEnableEngineBody();
        body.type = engineType;
        body.description = description;
        body.config = new VaultEnableEngineBody.Config();
        body.config.defaultLeaseTimeToLive = options.defaultLeaseTimeToLive;
        body.config.maxLeaseTimeToLive = options.maxLeaseTimeToLive;
        body.config.forceNoCache = options.forceNoCache;
        body.config.auditNonHMACRequestKeys = options.auditNonHMACRequestKeys;
        body.config.auditNonHMACResponseKeys = options.auditNonHMACResponseKeys;
        body.config.listingVisibility = options.listingVisibility != null ? options.listingVisibility.name().toLowerCase()
                : null;
        body.config.passthroughRequestHeaders = options.passthroughRequestHeaders;
        body.config.allowedResponseHeaders = options.allowedResponseHeaders;
        body.config.pluginVersion = options.pluginVersion;
        body.config.allowedManagedKeys = options.allowedManagedKeys;
        body.options = options.options;

        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.enableEngine(vaultClient, token, mount, body);
        });
    }

    @Override
    public Uni<Void> disable(String mount) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.disableEngine(vaultClient, token, mount);
        });
    }

    @Override
    public Uni<VaultPlugins> listPlugins() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.listPlugins(vaultClient, token).map(r -> new VaultPlugins()
                    .setAuth(r.data.auth)
                    .setDatabase(r.data.database)
                    .setSecret(r.data.secret)
                    .setDetailed(r.data.detailed.stream().map(d -> new VaultPluginDetails()
                            .setBuiltin(d.builtin)
                            .setDeprecationStatus(d.deprecationStatus)
                            .setName(d.name)
                            .setType(d.type)
                            .setVersion(d.version)
                            .setSha256(d.sha256)
                            .setCommand(d.command)
                            .setArgs(d.args)
                            .setEnv(d.env)).collect(Collectors.toList())));
        });
    }

    @Override
    public Uni<List<String>> listPlugins(String type) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.listPlugins(vaultClient, token, type).map(r -> r.data.keys);
        });
    }

    @Override
    public Uni<VaultPluginDetails> getPluginDetails(String type, String name, @Nullable String version) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.getPluginDetails(vaultClient, token, type, name, version)
                    .map(r -> new VaultPluginDetails()
                            .setBuiltin(r.data.builtin)
                            .setDeprecationStatus(r.data.deprecationStatus)
                            .setName(r.data.name)
                            .setType(r.data.type)
                            .setVersion(r.data.version)
                            .setSha256(r.data.sha256)
                            .setCommand(r.data.command)
                            .setArgs(r.data.args)
                            .setEnv(r.data.env));
        })
                .onFailure(VaultClientException.class).recoverWithUni(x -> {
                    VaultClientException vx = (VaultClientException) x;
                    if (vx.getStatus() == 404) {
                        return Uni.createFrom().nullItem();
                    } else {
                        return Uni.createFrom().failure(x);
                    }
                });
    }

    @Override
    public Uni<Void> registerPlugin(String type, String name, @Nullable String version, String sha256, String command,
            @Nullable List<String> args, @Nullable List<String> env) {
        var body = new VaultRegisterPluginBody();
        body.version = version;
        body.sha256 = sha256;
        body.command = command;
        body.args = args;
        body.env = env;

        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.registerPlugin(vaultClient, token, type, name, body);
        });
    }

    @Override
    public Uni<Void> removePlugin(String type, String name, @Nullable String version) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalSystemBackend.removePlugin(vaultClient, token, type, name, version);
        });
    }
}

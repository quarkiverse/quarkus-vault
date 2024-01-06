package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.DurationHelper.*;
import static java.util.stream.Collectors.toMap;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultSystemBackendReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.sys.init.VaultSysInitParams;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsEnableConfig;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsListingVisibility;
import io.quarkus.vault.client.api.sys.mounts.VaultSysMountsTuneParams;
import io.quarkus.vault.client.api.sys.plugins.VaultSysPluginsRegisterParams;
import io.quarkus.vault.client.api.sys.policy.VaultSysPolicyReadResultData;
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

    @Override
    public Uni<VaultInit> init(int secretShares, int secretThreshold) {
        return vaultClient.sys().init().init(new VaultSysInitParams()
                .setSecretShares(secretShares)
                .setSecretThreshold(secretThreshold))
                .map(init -> new VaultInit(init.getKeys(), init.getKeysBase64(), init.getRootToken()));
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
        return vaultClient.sys().seal().status()
                .map(result -> {
                    final VaultSealStatus vaultSealStatus = new VaultSealStatus();
                    vaultSealStatus.setClusterId(result.getClusterId());
                    vaultSealStatus.setClusterName(result.getClusterName());
                    vaultSealStatus.setInitialized(result.isInitialized());
                    vaultSealStatus.setMigration(result.isMigration());
                    vaultSealStatus.setN(result.getN());
                    vaultSealStatus.setNonce(result.getNonce());
                    vaultSealStatus.setProgress(result.getProgress());
                    vaultSealStatus.setRecoverySeal(result.isRecoverySeal());
                    vaultSealStatus.setSealed(result.isSealed());
                    vaultSealStatus.setT(result.getT());
                    vaultSealStatus.setType(result.getType());
                    vaultSealStatus.setVersion(result.getVersion());

                    return vaultSealStatus;
                });
    }

    private Uni<VaultHealthStatus> healthStatus(boolean isStandByOk, boolean isPerfStandByOk) {
        return vaultClient.sys().health().info(isStandByOk, isPerfStandByOk)
                .map(result -> {

                    final VaultHealthStatus vaultHealthStatus = new VaultHealthStatus();
                    vaultHealthStatus.setClusterId(result.getClusterId());
                    vaultHealthStatus.setClusterName(result.getClusterName());
                    vaultHealthStatus.setInitialized(result.isInitialized());
                    vaultHealthStatus.setPerformanceStandby(result.isPerformanceStandby());
                    vaultHealthStatus.setReplicationDrMode(result.getReplicationDrMode());
                    vaultHealthStatus.setReplicationPerfMode(result.getReplicationPerformanceMode());
                    vaultHealthStatus.setSealed(result.isSealed());
                    vaultHealthStatus.setServerTimeUtc(result.getServerTimeUtc());
                    vaultHealthStatus.setStandby(result.isStandby());
                    vaultHealthStatus.setVersion(result.getVersion());

                    return vaultHealthStatus;
                });
    }

    private Uni<VaultHealth> health(boolean isStandByOk, boolean isPerfStandByOk) {
        return vaultClient.sys().health().statusCode(isStandByOk, isPerfStandByOk)
                .map(VaultHealth::new);
    }

    @Override
    public Uni<String> getPolicyRules(String name) {
        return vaultClient.sys().policy().read(name).map(VaultSysPolicyReadResultData::getRules);
    }

    @Override
    public Uni<Void> createUpdatePolicy(String name, String policy) {
        return vaultClient.sys().policy().update(name, policy);
    }

    @Override
    public Uni<Void> deletePolicy(String name) {
        return vaultClient.sys().policy().delete(name);
    }

    @Override
    public Uni<List<String>> getPolicies() {
        return vaultClient.sys().policy().list();
    }

    @Override
    public Uni<VaultSecretEngineInfo> getSecretEngineInfo(String mount) {
        return vaultClient.sys().mounts().read(mount)
                .map(result -> {
                    VaultSecretEngineInfo info = new VaultSecretEngineInfo();
                    info.setDescription(result.getDescription());
                    info.setType(result.getType());
                    info.setLocal(result.isLocal());
                    info.setExternalEntropyAccess(result.isExternalEntropyAccess());
                    info.setSealWrap(result.isSealWrap());
                    info.setPluginVersion(result.getPluginVersion());
                    info.setRunningPluginVersion(result.getRunningPluginVersion());
                    info.setRunningSha256(result.getRunningSha256());
                    info.setDefaultLeaseTimeToLive(toLongDurationSeconds(result.getConfig().getDefaultLeaseTtl()));
                    info.setMaxLeaseTimeToLive(toLongDurationSeconds(result.getConfig().getMaxLeaseTtl()));
                    info.setForceNoCache(result.getConfig().isForceNoCache());
                    info.setOptions(result.getOptions());
                    info.setAuditNonHMACRequestKeys(result.getConfig().getAuditNonHmacRequestKeys());
                    info.setAuditNonHMACResponseKeys(result.getConfig().getAuditNonHmacResponseKeys());
                    info.setListingVisibility(result.getConfig().getListingVisibility() != null
                            ? EngineListingVisibility.from(result.getConfig().getListingVisibility().getValue())
                            : null);
                    info.setPassthroughRequestHeaders(result.getConfig().getPassthroughRequestHeaders());
                    info.setAllowedResponseHeaders(result.getConfig().getAllowedResponseHeaders());
                    info.setAllowedManagedKeys(result.getConfig().getAllowedManagedKeys());
                    return info;
                });
    }

    public Uni<VaultTuneInfo> getTuneInfo(String mount) {
        return vaultClient.sys().mounts().readTune(mount)
                .map(result -> {
                    VaultTuneInfo tuneInfo = new VaultTuneInfo();
                    tuneInfo.setDefaultLeaseTimeToLive(toLongDurationSeconds(result.getDefaultLeaseTtl()));
                    tuneInfo.setMaxLeaseTimeToLive(toLongDurationSeconds(result.getMaxLeaseTtl()));
                    tuneInfo.setDescription(result.getDescription());
                    tuneInfo.setForceNoCache(result.isForceNoCache());
                    tuneInfo.setOptions(result.getOptions());
                    tuneInfo.setAuditNonHMACRequestKeys(result.getAuditNonHmacRequestKeys());
                    tuneInfo.setAuditNonHMACResponseKeys(result.getAuditNonHmacResponseKeys());
                    tuneInfo.setListingVisibility(result.getListingVisibility() != null
                            ? EngineListingVisibility.from(result.getListingVisibility().getValue())
                            : null);
                    tuneInfo.setPassthroughRequestHeaders(result.getPassthroughRequestHeaders());
                    tuneInfo.setAllowedResponseHeaders(result.getAllowedResponseHeaders());
                    tuneInfo.setAllowedManagedKeys(result.getAllowedManagedKeys());
                    return tuneInfo;
                });
    }

    @Override
    public Uni<Void> updateTuneInfo(String mount, VaultTuneInfo tuneInfoUpdates) {
        var params = new VaultSysMountsTuneParams()
                .setDescription(tuneInfoUpdates.getDescription())
                .setDefaultLeaseTtl(fromSeconds(tuneInfoUpdates.getDefaultLeaseTimeToLive()))
                .setMaxLeaseTtl(fromSeconds(tuneInfoUpdates.getMaxLeaseTimeToLive()))
                .setAuditNonHmacRequestKeys(tuneInfoUpdates.getAuditNonHMACRequestKeys())
                .setAuditNonHmacResponseKeys(tuneInfoUpdates.getAuditNonHMACResponseKeys())
                .setListingVisibility(tuneInfoUpdates.getListingVisibility() != null
                        ? VaultSysMountsListingVisibility.from(tuneInfoUpdates.getListingVisibility().getValue())
                        : null)
                .setPassthroughRequestHeaders(tuneInfoUpdates.getPassthroughRequestHeaders())
                .setAllowedResponseHeaders(tuneInfoUpdates.getAllowedResponseHeaders())
                .setAllowedManagedKeys(tuneInfoUpdates.getAllowedManagedKeys());
        return vaultClient.sys().mounts().tune(mount, params);
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
        var config = new VaultSysMountsEnableConfig()
                .setDefaultLeaseTtl(fromVaultDuration(options.defaultLeaseTimeToLive))
                .setMaxLeaseTtl(fromVaultDuration(options.maxLeaseTimeToLive))
                .setForceNoCache(options.forceNoCache)
                .setAuditNonHmacRequestKeys(options.auditNonHMACRequestKeys)
                .setAuditNonHmacResponseKeys(options.auditNonHMACResponseKeys)
                .setListingVisibility(options.listingVisibility != null
                        ? VaultSysMountsListingVisibility.from(options.listingVisibility.getValue())
                        : null)
                .setPassthroughRequestHeaders(options.passthroughRequestHeaders)
                .setAllowedResponseHeaders(options.allowedResponseHeaders)
                .setAllowedManagedKeys(options.allowedManagedKeys)
                .setPluginVersion(options.pluginVersion);
        return vaultClient.sys().mounts().enable(mount, engineType, description, config, options.options != null
                ? options.options.entrySet().stream().collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
                : null);
    }

    @Override
    public Uni<Void> disable(String mount) {
        return vaultClient.sys().mounts().disable(mount);
    }

    @Override
    public Uni<VaultPlugins> listPlugins() {
        return vaultClient.sys().plugins().list()
                .map(r -> new VaultPlugins()
                        .setAuth(r.getAuth())
                        .setDatabase(r.getDatabase())
                        .setSecret(r.getSecret())
                        .setDetailed(r.getDetailed().stream().map(d -> new VaultPluginDetails()
                                .setBuiltin(d.isBuiltin())
                                .setDeprecationStatus(d.getDeprecationStatus())
                                .setName(d.getName())
                                .setType(d.getType())
                                .setSha256(d.getSha256())
                                .setVersion(d.getVersion())).collect(Collectors.toList())));
    }

    @Override
    public Uni<List<String>> listPlugins(String type) {
        switch (type.toLowerCase()) {
            case "auth":
                return vaultClient.sys().plugins().listAuth();
            case "database":
                return vaultClient.sys().plugins().listDatabase();
            case "secret":
                return vaultClient.sys().plugins().listSecret();
        }
        return Uni.createFrom().failure(new IllegalArgumentException("Unknown plugin type: " + type));
    }

    @Override
    public Uni<VaultPluginDetails> getPluginDetails(String type, String name, @Nullable String version) {
        return vaultClient.sys().plugins().read(type, name, version)
                .map(r -> new VaultPluginDetails()
                        .setBuiltin(r.isBuiltin())
                        .setName(r.getName())
                        .setVersion(r.getVersion())
                        .setSha256(r.getSha256())
                        .setCommand(r.getCommand())
                        .setArgs(r.getArgs())
                        .setDeprecationStatus(r.getDeprecationStatus()))
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
        var params = new VaultSysPluginsRegisterParams()
                .setVersion(version)
                .setSha256(sha256)
                .setCommand(command)
                .setArgs(args)
                .setEnv(env);

        return vaultClient.sys().plugins().register(type, name, params);
    }

    @Override
    public Uni<Void> removePlugin(String type, String name, @Nullable String version) {
        return vaultClient.sys().plugins().remove(type, name, version);
    }
}

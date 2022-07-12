package io.quarkus.vault.runtime;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultSystemBackendReactiveEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.dto.sys.VaultEnableEngineBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneBody;
import io.quarkus.vault.runtime.config.VaultBuildTimeConfig;
import io.quarkus.vault.sys.EnableEngineOptions;
import io.quarkus.vault.sys.VaultHealth;
import io.quarkus.vault.sys.VaultHealthStatus;
import io.quarkus.vault.sys.VaultInit;
import io.quarkus.vault.sys.VaultSealStatus;
import io.quarkus.vault.sys.VaultSecretEngine;
import io.quarkus.vault.sys.VaultSecretEngineInfo;
import io.quarkus.vault.sys.VaultTuneInfo;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultSystemBackendManager implements VaultSystemBackendReactiveEngine {

    @Inject
    private VaultClient vaultClient;
    @Inject
    private VaultBuildTimeConfig buildTimeConfig;
    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalSystemBackend vaultInternalSystemBackend;

    @Override
    public Uni<VaultInit> init(int secretShares, int secretThreshold) {
        return vaultInternalSystemBackend.init(vaultClient, secretShares, secretThreshold)
                .map(init -> new VaultInit(init.keys, init.keysBase64, init.rootToken));
    }

    @Override
    public Uni<VaultHealth> health() {
        boolean isStandByOk = this.buildTimeConfig.health.standByOk;
        boolean isPerfStandByOk = this.buildTimeConfig.health.performanceStandByOk;

        return this.health(isStandByOk, isPerfStandByOk);
    }

    @Override
    public Uni<VaultHealthStatus> healthStatus() {
        boolean isStandByOk = this.buildTimeConfig.health.standByOk;
        boolean isPerfStandByOk = this.buildTimeConfig.health.performanceStandByOk;

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
                    vaultSealStatus.setSealed(vaultSealStatusResult.sealed);
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
                    vaultHealthStatus.setSealed(vaultHealthResult.sealed);
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
                        info.setOptions(result.data.options);
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
}

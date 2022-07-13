package io.quarkus.vault.runtime.client.backend;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.sys.VaultEnableEngineBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultHealthResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultInitResponse;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultLeasesLookup;
import io.quarkus.vault.runtime.client.dto.sys.VaultListPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultPolicyResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultRenewLease;
import io.quarkus.vault.runtime.client.dto.sys.VaultSealStatusResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultSecretEngineInfoResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultTuneResult;
import io.quarkus.vault.runtime.client.dto.sys.VaultUnwrapBody;
import io.quarkus.vault.runtime.client.dto.sys.VaultWrapResult;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalSystemBackend extends VaultInternalBase {

    public Uni<Integer> systemHealth(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);
        return vaultClient.head("sys/health", queryParams);
    }

    public Uni<VaultHealthResult> systemHealthStatus(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);
        return vaultClient.get("sys/health", queryParams, VaultHealthResult.class);
    }

    public Uni<VaultSealStatusResult> systemSealStatus() {
        return vaultClient.get("sys/seal-status", emptyMap(), VaultSealStatusResult.class);
    }

    public Uni<VaultInitResponse> init(int secretShares, int secretThreshold) {
        VaultInitBody body = new VaultInitBody(secretShares, secretThreshold);
        return vaultClient.put("sys/init", body, VaultInitResponse.class);
    }

    public Uni<VaultWrapResult> wrap(String token, long ttl, Object object) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Vault-Wrap-TTL", "" + ttl);
        return vaultClient.post("sys/wrapping/wrap", token, headers, object, VaultWrapResult.class);
    }

    public <T> Uni<T> unwrap(String wrappingToken, Class<T> resultClass) {
        return vaultClient.post("sys/wrapping/unwrap", wrappingToken, VaultUnwrapBody.EMPTY, resultClass);
    }

    public Uni<VaultPolicyResult> getPolicy(String token, String name) {
        return vaultClient.get("sys/policy/" + name, token, VaultPolicyResult.class);
    }

    public Uni<Void> createUpdatePolicy(String token, String name, VaultPolicyBody body) {
        return vaultClient.put("sys/policy/" + name, token, body, 204);
    }

    public Uni<VaultListPolicyResult> listPolicies(String token) {
        return vaultClient.get("sys/policy", token, VaultListPolicyResult.class);
    }

    public Uni<Void> deletePolicy(String token, String name) {
        return vaultClient.delete("sys/policy/" + name, token, 204);
    }

    public Uni<VaultLeasesLookup> lookupLease(String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return vaultClient.put("sys/leases/lookup", token, body, VaultLeasesLookup.class);
    }

    public Uni<VaultRenewLease> renewLease(String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return vaultClient.put("sys/leases/renew", token, body, VaultRenewLease.class);
    }

    public Uni<Void> enableEngine(String token, String mount, VaultEnableEngineBody body) {
        return vaultClient.post("sys/mounts/" + mount, token, body, 204);
    }

    public Uni<Void> disableEngine(String token, String mount) {
        return vaultClient.delete("sys/mounts/" + mount, token, 204);
    }

    public Uni<VaultTuneResult> getTuneInfo(String token, String mount) {
        return vaultClient.get("sys/mounts/" + mount + "/tune", token, VaultTuneResult.class);
    }

    public Uni<Void> updateTuneInfo(String token, String mount, VaultTuneBody body) {
        return vaultClient.post("sys/mounts/" + mount + "/tune", token, body, 204);
    }

    public Uni<VaultSecretEngineInfoResult> getSecretEngineInfo(String token, String mount) {
        return vaultClient.get("sys/mounts/" + mount, token, VaultSecretEngineInfoResult.class);
    }

    // ---

    private Map<String, String> getHealthParams(boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = new HashMap<>();
        if (isStandByOk) {
            queryParams.put("standbyok", "true");
        }

        if (isPerfStandByOk) {
            queryParams.put("perfstandbyok", "true");
        }

        return queryParams;
    }
}

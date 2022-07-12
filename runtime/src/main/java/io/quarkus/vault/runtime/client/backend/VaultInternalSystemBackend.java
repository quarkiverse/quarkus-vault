package io.quarkus.vault.runtime.client.backend;

import static java.util.Collections.emptyMap;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
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

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [SYS]";
    }

    public Uni<Integer> systemHealth(VaultClient vaultClient, boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);
        return vaultClient.head(opName("Health"), "sys/health", queryParams);
    }

    public Uni<VaultHealthResult> systemHealthStatus(VaultClient vaultClient, boolean isStandByOk, boolean isPerfStandByOk) {
        Map<String, String> queryParams = getHealthParams(isStandByOk, isPerfStandByOk);
        return vaultClient.get(opName("Health Status"), "sys/health", queryParams, VaultHealthResult.class);
    }

    public Uni<VaultSealStatusResult> systemSealStatus(VaultClient vaultClient) {
        return vaultClient.get(opName("Seal Status"), "sys/seal-status", emptyMap(), VaultSealStatusResult.class);
    }

    public Uni<VaultInitResponse> init(VaultClient vaultClient, int secretShares, int secretThreshold) {
        VaultInitBody body = new VaultInitBody(secretShares, secretThreshold);
        return vaultClient.put(opName("Initialize"), "sys/init", body, VaultInitResponse.class);
    }

    public Uni<VaultWrapResult> wrap(VaultClient vaultClient, String token, long ttl, Object object) {
        Map<String, String> headers = new HashMap<>();
        headers.put("X-Vault-Wrap-TTL", "" + ttl);
        return vaultClient.post(opName("Wrap"), "sys/wrapping/wrap", token, headers, object, VaultWrapResult.class);
    }

    public <T> Uni<T> unwrap(VaultClient vaultClient, String wrappingToken, Class<T> resultClass) {
        return vaultClient.post(opName("Unwrap"), "sys/wrapping/unwrap", wrappingToken, VaultUnwrapBody.EMPTY, resultClass);
    }

    public Uni<VaultPolicyResult> getPolicy(VaultClient vaultClient, String token, String name) {
        return vaultClient.get(opName("Get Policy"), "sys/policy/" + name, token, VaultPolicyResult.class);
    }

    public Uni<Void> createUpdatePolicy(VaultClient vaultClient, String token, String name, VaultPolicyBody body) {
        return vaultClient.put(opName("Update Policy"), "sys/policy/" + name, token, body, 204);
    }

    public Uni<VaultListPolicyResult> listPolicies(VaultClient vaultClient, String token) {
        return vaultClient.get(opName("List Policies"), "sys/policy", token, VaultListPolicyResult.class);
    }

    public Uni<Void> deletePolicy(VaultClient vaultClient, String token, String name) {
        return vaultClient.delete(opName("Delete Policy"), "sys/policy/" + name, token, 204);
    }

    public Uni<VaultLeasesLookup> lookupLease(VaultClient vaultClient, String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return vaultClient.put(opName("Lookup Lease"), "sys/leases/lookup", token, body, VaultLeasesLookup.class);
    }

    public Uni<VaultRenewLease> renewLease(VaultClient vaultClient, String token, String leaseId) {
        VaultLeasesBody body = new VaultLeasesBody(leaseId);
        return vaultClient.put(opName("Renew Lease"), "sys/leases/renew", token, body, VaultRenewLease.class);
    }

    public Uni<Void> enableEngine(VaultClient vaultClient, String token, String mount, VaultEnableEngineBody body) {
        return vaultClient.post(opName("Enable Engine"), "sys/mounts/" + mount, token, body, 204);
    }

    public Uni<Void> disableEngine(VaultClient vaultClient, String token, String mount) {
        return vaultClient.delete(opName("Disable Engine"), "sys/mounts/" + mount, token, 204);
    }

    public Uni<VaultTuneResult> getTuneInfo(VaultClient vaultClient, String token, String mount) {
        return vaultClient.get(opName("Tune Info"), "sys/mounts/" + mount + "/tune", token, VaultTuneResult.class);
    }

    public Uni<Void> updateTuneInfo(VaultClient vaultClient, String token, String mount, VaultTuneBody body) {
        return vaultClient.post(opName("Tune"), "sys/mounts/" + mount + "/tune", token, body, 204);
    }

    public Uni<VaultSecretEngineInfoResult> getSecretEngineInfo(VaultClient vaultClient, String token, String mount) {
        return vaultClient.get(opName("Engine Info"), "sys/mounts/" + mount, token, VaultSecretEngineInfoResult.class);
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

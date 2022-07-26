package io.quarkus.vault.runtime.client.secretengine;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitDecryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncrypt;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitEncryptBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyConfigBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitKeyExport;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitListKeysResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitReadKeyResult;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRewrapBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSign;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitSignBody;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerify;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitVerifyBody;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalTransitSecretEngine extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [TRANSIT]";
    }

    public Uni<Void> updateTransitKeyConfiguration(VaultClient vaultClient, String token, String keyName,
            VaultTransitKeyConfigBody body) {
        return vaultClient.post(opName("Configure Key"), "transit/keys/" + keyName + "/config", token, body, 204);
    }

    public Uni<Void> createTransitKey(VaultClient vaultClient, String token, String keyName, VaultTransitCreateKeyBody body) {
        return vaultClient.post(opName("Create Key"), "transit/keys/" + keyName, token, body, 204);
    }

    public Uni<Void> deleteTransitKey(VaultClient vaultClient, String token, String keyName) {
        return vaultClient.delete(opName("Delete Key"), "transit/keys/" + keyName, token, 204);
    }

    public Uni<VaultTransitKeyExport> exportTransitKey(VaultClient vaultClient, String token, String keyType, String keyName,
            String version) {
        String path = "transit/export/" + keyType + "/" + keyName + (version != null ? "/" + version : "");
        return vaultClient.get(opName("Export Key"), path, token, VaultTransitKeyExport.class);
    }

    public Uni<VaultTransitReadKeyResult> readTransitKey(VaultClient vaultClient, String token, String keyName) {
        return vaultClient.get(opName("Read Key"), "transit/keys/" + keyName, token, VaultTransitReadKeyResult.class);
    }

    public Uni<VaultTransitListKeysResult> listTransitKeys(VaultClient vaultClient, String token) {
        return vaultClient.list(opName("List Keys"), "transit/keys", token, VaultTransitListKeysResult.class);
    }

    public Uni<VaultTransitEncrypt> encrypt(VaultClient vaultClient, String token, String keyName,
            VaultTransitEncryptBody body) {
        return vaultClient.post(opName("Encrypt Key"), "transit/encrypt/" + keyName, token, body, VaultTransitEncrypt.class);
    }

    public Uni<VaultTransitDecrypt> decrypt(VaultClient vaultClient, String token, String keyName,
            VaultTransitDecryptBody body) {
        return vaultClient.post(opName("Decrypt Key"), "transit/decrypt/" + keyName, token, body, VaultTransitDecrypt.class);
    }

    public Uni<VaultTransitSign> sign(VaultClient vaultClient, String token, String keyName, String hashAlgorithm,
            VaultTransitSignBody body) {
        String path = "transit/sign/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return vaultClient.post(opName("Sign"), path, token, body, VaultTransitSign.class);
    }

    public Uni<VaultTransitVerify> verify(VaultClient vaultClient, String token, String keyName, String hashAlgorithm,
            VaultTransitVerifyBody body) {
        String path = "transit/verify/" + keyName + (hashAlgorithm == null ? "" : "/" + hashAlgorithm);
        return vaultClient.post(opName("Verify"), path, token, body, VaultTransitVerify.class);
    }

    public Uni<VaultTransitEncrypt> rewrap(VaultClient vaultClient, String token, String keyName, VaultTransitRewrapBody body) {
        return vaultClient.post(opName("Rewrap"), "transit/rewrap/" + keyName, token, body, VaultTransitEncrypt.class);
    }
}

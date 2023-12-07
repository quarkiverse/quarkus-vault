package io.quarkus.vault.runtime.client.secretengine;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.transit.*;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Singleton;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class VaultInternalTransitSecretEngine extends VaultInternalBase {

    /**
     * Concatenates the provided URL path segments and sanitizes separators.
     * The path is returned without a leading "/".
     *
     * @param firstPart first URL path segment
     * @param otherParts other URL path segments
     * @return String concatenated path
     */
    private String getPath(String firstPart, String... otherParts) {
        String path = Stream.of(Paths.get(firstPart, otherParts))
                .map(Path::toString)
                .collect(Collectors.joining("/"));
        return (path.startsWith("/")) ? path.substring(1) : path;
    }

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [TRANSIT]";
    }

    public Uni<Void> updateTransitKeyConfiguration(VaultClient vaultClient, String token, String mount, String keyName,
            VaultTransitKeyConfigBody body) {
        return vaultClient.post(opName("Configure Key"), getPath(mount, "keys", keyName, "config"), token, body, 204);
    }

    public Uni<Void> createTransitKey(VaultClient vaultClient, String token, String mount, String keyName, VaultTransitCreateKeyBody body) {
        return vaultClient.post(opName("Create Key"), getPath(mount, "keys", keyName), token, body, 204);
    }

    public Uni<Void> deleteTransitKey(VaultClient vaultClient, String token, String mount, String keyName) {
        return vaultClient.delete(opName("Delete Key"), getPath(mount, "keys", keyName), token, 204);
    }

    public Uni<VaultTransitKeyExport> exportTransitKey(VaultClient vaultClient, String token, String mount, String keyType, String keyName,
            String version) {
        String path = getPath(mount, "export", keyType, keyName, version != null ? version : "");
        return vaultClient.get(opName("Export Key"), path, token, VaultTransitKeyExport.class);
    }

    public Uni<VaultTransitReadKeyResult> readTransitKey(VaultClient vaultClient, String token, String mount, String keyName) {
        return vaultClient.get(opName("Read Key"), getPath(mount, "keys", keyName), token, VaultTransitReadKeyResult.class);
    }

    public Uni<VaultTransitListKeysResult> listTransitKeys(VaultClient vaultClient, String token, String mount) {
        return vaultClient.list(opName("List Keys"), getPath(mount, "keys"), token, VaultTransitListKeysResult.class);
    }

    public Uni<VaultTransitEncrypt> encrypt(VaultClient vaultClient, String token, String mount, String keyName,
            VaultTransitEncryptBody body) {
        return vaultClient.post(opName("Encrypt Key"), getPath(mount, "encrypt", keyName), token, body, VaultTransitEncrypt.class);
    }

    public Uni<VaultTransitDecrypt> decrypt(VaultClient vaultClient, String token, String mount, String keyName,
            VaultTransitDecryptBody body) {
        return vaultClient.post(opName("Decrypt Key"), getPath(mount, "decrypt", keyName), token, body, VaultTransitDecrypt.class);
    }

    public Uni<VaultTransitSign> sign(VaultClient vaultClient, String token, String mount, String keyName, String hashAlgorithm,
            VaultTransitSignBody body) {
        String path = getPath(mount, "sign", keyName, hashAlgorithm == null ? "" : hashAlgorithm);
        return vaultClient.post(opName("Sign"), path, token, body, VaultTransitSign.class);
    }

    public Uni<VaultTransitVerify> verify(VaultClient vaultClient, String token, String mount, String keyName, String hashAlgorithm,
            VaultTransitVerifyBody body) {
        String path = getPath(mount, "verify", keyName, hashAlgorithm == null ? "" : hashAlgorithm);
        return vaultClient.post(opName("Verify"), path, token, body, VaultTransitVerify.class);
    }

    public Uni<VaultTransitEncrypt> rewrap(VaultClient vaultClient, String token, String mount, String keyName, VaultTransitRewrapBody body) {
        return vaultClient.post(opName("Rewrap"), getPath(mount, "rewrap", keyName), token, body, VaultTransitEncrypt.class);
    }
}

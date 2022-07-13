package io.quarkus.vault.runtime.client.secretengine;

import java.util.Optional;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultInternalBase;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPGenerateCodeResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPListKeysResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPReadKeyResult;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeBody;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPValidateCodeResult;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultInternalTOPTSecretEngine extends VaultInternalBase {

    public Uni<Optional<VaultTOTPCreateKeyResult>> createTOTPKey(String token, String keyName, VaultTOTPCreateKeyBody body) {
        String path = "totp/keys/" + keyName;

        // Depending on parameters it might produce an output or not
        if (body.isProducingOutput()) {
            return vaultClient.post(path, token, body, VaultTOTPCreateKeyResult.class, 200)
                    .map(Optional::of);
        } else {
            return vaultClient.post(path, token, body, 204)
                    .map(v -> Optional.empty());
        }
    }

    public Uni<VaultTOTPReadKeyResult> readTOTPKey(String token, String keyName) {
        String path = "totp/keys/" + keyName;
        return vaultClient.get(path, token, VaultTOTPReadKeyResult.class);
    }

    public Uni<VaultTOTPListKeysResult> listTOTPKeys(String token) {
        return vaultClient.list("totp/keys", token, VaultTOTPListKeysResult.class);
    }

    public Uni<Void> deleteTOTPKey(String token, String keyName) {
        String path = "totp/keys/" + keyName;
        return vaultClient.delete(path, token, 204);
    }

    public Uni<VaultTOTPGenerateCodeResult> generateTOTPCode(String token, String keyName) {
        String path = "totp/code/" + keyName;
        return vaultClient.get(path, token, VaultTOTPGenerateCodeResult.class);
    }

    public Uni<VaultTOTPValidateCodeResult> validateTOTPCode(String token, String keyName, String code) {
        String path = "totp/code/" + keyName;
        VaultTOTPValidateCodeBody body = new VaultTOTPValidateCodeBody(code);
        return vaultClient.post(path, token, body, VaultTOTPValidateCodeResult.class);
    }
}

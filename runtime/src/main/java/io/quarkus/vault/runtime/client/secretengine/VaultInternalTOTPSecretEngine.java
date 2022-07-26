package io.quarkus.vault.runtime.client.secretengine;

import java.util.Optional;

import javax.inject.Singleton;

import io.quarkus.vault.runtime.client.VaultClient;
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
public class VaultInternalTOTPSecretEngine extends VaultInternalBase {

    @Override
    protected String opNamePrefix() {
        return super.opNamePrefix() + " [TOTP]";
    }

    public Uni<Optional<VaultTOTPCreateKeyResult>> createTOTPKey(VaultClient vaultClient, String token, String keyName,
            VaultTOTPCreateKeyBody body) {
        String path = "totp/keys/" + keyName;

        // Depending on parameters it might produce an output or not
        if (body.isProducingOutput()) {
            return vaultClient.post(opName("Create Key"), path, token, body, VaultTOTPCreateKeyResult.class, 200)
                    .map(Optional::of);
        } else {
            return vaultClient.post(opName("Create Key"), path, token, body, 204)
                    .map(v -> Optional.empty());
        }
    }

    public Uni<VaultTOTPReadKeyResult> readTOTPKey(VaultClient vaultClient, String token, String keyName) {
        String path = "totp/keys/" + keyName;
        return vaultClient.get(opName("Read Key"), path, token, VaultTOTPReadKeyResult.class);
    }

    public Uni<VaultTOTPListKeysResult> listTOTPKeys(VaultClient vaultClient, String token) {
        return vaultClient.list(opName("List Keys"), "totp/keys", token, VaultTOTPListKeysResult.class);
    }

    public Uni<Void> deleteTOTPKey(VaultClient vaultClient, String token, String keyName) {
        String path = "totp/keys/" + keyName;
        return vaultClient.delete(opName("Delete Key"), path, token, 204);
    }

    public Uni<VaultTOTPGenerateCodeResult> generateTOTPCode(VaultClient vaultClient, String token, String keyName) {
        String path = "totp/code/" + keyName;
        return vaultClient.get(opName("Generate Code"), path, token, VaultTOTPGenerateCodeResult.class);
    }

    public Uni<VaultTOTPValidateCodeResult> validateTOTPCode(VaultClient vaultClient, String token, String keyName,
            String code) {
        String path = "totp/code/" + keyName;
        VaultTOTPValidateCodeBody body = new VaultTOTPValidateCodeBody(code);
        return vaultClient.post(opName("Validate Code"), path, token, body, VaultTOTPValidateCodeResult.class);
    }
}

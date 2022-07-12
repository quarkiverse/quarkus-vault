package io.quarkus.vault.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import io.quarkus.vault.VaultTOTPSecretReactiveEngine;
import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.dto.totp.VaultTOTPCreateKeyBody;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalTOPTSecretEngine;
import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultTOTPManager implements VaultTOTPSecretReactiveEngine {

    @Inject
    private VaultClient vaultClient;
    @Inject
    private VaultAuthManager vaultAuthManager;
    @Inject
    private VaultInternalTOPTSecretEngine vaultInternalTOPTSecretEngine;

    @Override
    public Uni<Optional<KeyDefinition>> createKey(String name, CreateKeyParameters createKeyParameters) {
        VaultTOTPCreateKeyBody body = new VaultTOTPCreateKeyBody();

        body.accountName = createKeyParameters.getAccountName();
        body.algorithm = createKeyParameters.getAlgorithm();
        body.digits = createKeyParameters.getDigits();
        body.exported = createKeyParameters.getExported();
        body.generate = createKeyParameters.getGenerate();
        body.issuer = createKeyParameters.getIssuer();
        body.key = createKeyParameters.getKey();
        body.keySize = createKeyParameters.getKeySize();
        body.period = createKeyParameters.getPeriod();
        body.qrSize = createKeyParameters.getQrSize();
        body.skew = createKeyParameters.getSkew();
        body.url = createKeyParameters.getUrl();

        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.createTOTPKey(vaultClient, token, name, body)
                    .map(opt -> opt.map(result -> new KeyDefinition(result.data.barcode, result.data.url)));
        });
    }

    @Override
    public Uni<KeyConfiguration> readKey(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.readTOTPKey(vaultClient, token, name)
                    .map(result -> new KeyConfiguration(result.data.accountName,
                            result.data.algorithm, result.data.digits,
                            result.data.issuer, result.data.period));
        });
    }

    @Override
    public Uni<List<String>> listKeys() {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.listTOTPKeys(vaultClient, token)
                    .map(r -> r.data.keys)
                    .onFailure(VaultClientException.class).recoverWithUni(e -> {
                        if (((VaultClientException) e).getStatus() == 404) {
                            return Uni.createFrom().item(Collections.emptyList());
                        }
                        return Uni.createFrom().failure(e);
                    });
        });
    }

    @Override
    public Uni<Void> deleteKey(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.deleteTOTPKey(vaultClient, token, name);
        });
    }

    @Override
    public Uni<String> generateCode(String name) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.generateTOTPCode(vaultClient, token, name).map(r -> r.data.code);
        });
    }

    @Override
    public Uni<Boolean> validateCode(String name, String code) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            return vaultInternalTOPTSecretEngine.validateTOTPCode(vaultClient, token, name, code).map(r -> r.data.valid);
        });
    }
}

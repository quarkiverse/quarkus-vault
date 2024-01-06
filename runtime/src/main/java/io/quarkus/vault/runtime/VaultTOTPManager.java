package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.DurationHelper.fromVaultDuration;

import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultTOTPSecretReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.api.secrets.totp.VaultSecretsTOTP;
import io.quarkus.vault.client.api.secrets.totp.VaultSecretsTOTPCreateKeyParams;
import io.quarkus.vault.secrets.totp.CreateKeyParameters;
import io.quarkus.vault.secrets.totp.KeyConfiguration;
import io.quarkus.vault.secrets.totp.KeyDefinition;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultTOTPManager implements VaultTOTPSecretReactiveEngine {

    private final VaultSecretsTOTP totp;

    @Inject
    public VaultTOTPManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        this.totp = vaultClient.secrets().totp();
    }

    @Override
    public Uni<Optional<KeyDefinition>> createKey(String name, CreateKeyParameters createKeyParameters) {

        var params = new VaultSecretsTOTPCreateKeyParams()
                .setAccountName(createKeyParameters.getAccountName())
                .setAlgorithm(createKeyParameters.getAlgorithm())
                .setDigits(createKeyParameters.getDigits())
                .setExported(createKeyParameters.getExported())
                .setGenerate(createKeyParameters.getGenerate())
                .setIssuer(createKeyParameters.getIssuer())
                .setKey(createKeyParameters.getKey())
                .setKeySize(createKeyParameters.getKeySize())
                .setPeriod(fromVaultDuration(createKeyParameters.getPeriod()))
                .setQrSize(createKeyParameters.getQrSize())
                .setSkew(createKeyParameters.getSkew())
                .setUrl(createKeyParameters.getUrl());

        return totp.createKey(name, params)
                .map(opt -> opt.map(result -> new KeyDefinition(result.getBarcode(), result.getUrl())));
    }

    @Override
    public Uni<KeyConfiguration> readKey(String name) {
        return totp.readKey(name).map(result -> new KeyConfiguration(result.getAccountName(),
                result.getAlgorithm(), result.getDigits(),
                result.getIssuer(), result.getPeriod() != null ? (int) result.getPeriod().toSeconds() : 0));
    }

    @Override
    public Uni<List<String>> listKeys() {
        return totp.listKeys();
    }

    @Override
    public Uni<Void> deleteKey(String name) {
        return totp.deleteKey(name);
    }

    @Override
    public Uni<String> generateCode(String name) {
        return totp.generateCode(name);
    }

    @Override
    public Uni<Boolean> validateCode(String name, String code) {
        return totp.validateCode(name, code);
    }
}

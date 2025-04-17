package io.quarkus.vault.runtime;

import static io.quarkus.vault.runtime.SigningRequestResultPair.NO_KEY_VERSION;
import static io.quarkus.vault.transit.VaultTransitSecretEngineConstants.INVALID_SIGNATURE;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.singletonList;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.quarkus.vault.VaultTransitSecretReactiveEngine;
import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultException;
import io.quarkus.vault.client.api.common.VaultHashAlgorithm;
import io.quarkus.vault.client.api.secrets.transit.*;
import io.quarkus.vault.runtime.config.TransitKeyConfig;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.quarkus.vault.runtime.transit.DecryptionResult;
import io.quarkus.vault.runtime.transit.EncryptionResult;
import io.quarkus.vault.runtime.transit.SigningResult;
import io.quarkus.vault.runtime.transit.VaultTransitBatchResult;
import io.quarkus.vault.runtime.transit.VerificationResult;
import io.quarkus.vault.transit.ClearData;
import io.quarkus.vault.transit.DecryptionRequest;
import io.quarkus.vault.transit.EncryptionRequest;
import io.quarkus.vault.transit.KeyConfigRequestDetail;
import io.quarkus.vault.transit.KeyCreationRequestDetail;
import io.quarkus.vault.transit.RewrappingRequest;
import io.quarkus.vault.transit.SignVerifyOptions;
import io.quarkus.vault.transit.SigningInput;
import io.quarkus.vault.transit.SigningRequest;
import io.quarkus.vault.transit.TransitContext;
import io.quarkus.vault.transit.VaultDecryptionBatchException;
import io.quarkus.vault.transit.VaultEncryptionBatchException;
import io.quarkus.vault.transit.VaultRewrappingBatchException;
import io.quarkus.vault.transit.VaultSigningBatchException;
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitAsymmetricKeyVersion;
import io.quarkus.vault.transit.VaultTransitDataKey;
import io.quarkus.vault.transit.VaultTransitDataKeyRequestDetail;
import io.quarkus.vault.transit.VaultTransitDataKeyType;
import io.quarkus.vault.transit.VaultTransitExportKeyType;
import io.quarkus.vault.transit.VaultTransitKeyDetail;
import io.quarkus.vault.transit.VaultTransitKeyExportDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyDetail;
import io.quarkus.vault.transit.VaultTransitSymmetricKeyVersion;
import io.quarkus.vault.transit.VaultVerificationBatchException;
import io.quarkus.vault.transit.VerificationRequest;
import io.quarkus.vault.utils.Plugs;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class VaultTransitManager implements VaultTransitSecretReactiveEngine {

    private final VaultSecretsTransit transit;
    private final VaultConfigHolder vaultConfigHolder;

    @Inject
    public VaultTransitManager(VaultClient client, VaultConfigHolder configHolder) {
        this.transit = client.secrets().transit(configHolder.getVaultRuntimeConfig().transitSecretEngineMountPath());
        this.vaultConfigHolder = configHolder;
    }

    private VaultRuntimeConfig getConfig() {
        return vaultConfigHolder.getVaultRuntimeConfig();
    }

    @Override
    public Uni<String> encrypt(String keyName, String clearData) {
        return encrypt(keyName, new ClearData(clearData), null);
    }

    @Override
    public Uni<String> encrypt(String keyName, ClearData clearData, TransitContext transitContext) {
        EncryptionRequest item = new EncryptionRequest(clearData, transitContext);
        return encryptBatch(keyName, singletonList(item)).map(results -> results.get(0).getValueOrElseError());
    }

    // workaround https://github.com/hashicorp/vault/issues/10232
    private Uni<String> encrypt(String keyName, EncryptionRequest request) {

        var params = new VaultSecretsTransitEncryptParams()
                .setPlaintext(request.getData().getValue())
                .setContext(request.getContext())
                .setKeyVersion(request.getKeyVersion());

        TransitKeyConfig config = getTransitConfig(keyName);

        String configKeyName;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
            params.setType(config.type().map(VaultSecretsTransitKeyType::from).orElse(null));
            params.setConvergentEncryption(config.convergentEncryption().map(Boolean::valueOf).orElse(null));
        } else {
            configKeyName = keyName;
        }

        return Uni.createFrom().completionStage(transit.encrypt(configKeyName, params))
                .map(VaultSecretsTransitEncryptResultData::getCiphertext);
    }

    private TransitKeyConfig getTransitConfig(String keyName) {
        return getConfig().transit().key().get(keyName);
    }

    @Override
    public Uni<Map<EncryptionRequest, String>> encrypt(String keyName, List<EncryptionRequest> requests) {
        if (requests.size() == 1) {
            EncryptionRequest request = requests.get(0);
            return encrypt(keyName, request)
                    .map(result -> Map.of(request, result));
        }
        return encryptBatch(keyName, requests)
                .map(results -> {
                    checkBatchErrors(results,
                            errors -> new VaultEncryptionBatchException(errors + " encryption errors",
                                    zip(requests, results)));
                    return zipRequestToValue(requests, results);
                });
    }

    private Uni<List<EncryptionResult>> encryptBatch(String keyName, List<EncryptionRequest> requests) {

        var params = new VaultSecretsTransitEncryptBatchParams();
        params.setBatchInput(requests.stream().map(r -> new VaultSecretsTransitEncryptBatchItem()
                .setPlaintext(r.getData().getValue())
                .setContext(r.getContext())
                .setKeyVersion(r.getKeyVersion()))
                .collect(toList()));

        TransitKeyConfig config = getTransitConfig(keyName);

        String configKeyName;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
            params.setType(config.type().map(VaultSecretsTransitKeyType::from).orElse(null));
            params.setConvergentEncryption(config.convergentEncryption().map(Boolean::valueOf).orElse(null));
        } else {
            configKeyName = keyName;
        }

        return Uni.createFrom().completionStage(transit.encryptBatch(configKeyName, params))
                .map(result -> result.stream()
                        .map(r -> new EncryptionResult(r.getCiphertext(), r.getError()))
                        .collect(toList()));
    }

    @Override
    public Uni<ClearData> decrypt(String keyName, String ciphertext) {
        return decrypt(keyName, ciphertext, null);
    }

    @Override
    public Uni<ClearData> decrypt(String keyName, String ciphertext, TransitContext transitContext) {
        DecryptionRequest item = new DecryptionRequest(ciphertext, transitContext);
        return decryptBatch(keyName, singletonList(item)).map(results -> results.get(0).getValueOrElseError());
    }

    @Override
    public Uni<Map<DecryptionRequest, ClearData>> decrypt(String keyName, List<DecryptionRequest> requests) {
        return decryptBatch(keyName, requests)
                .map(results -> {
                    checkBatchErrors(results,
                            errors -> new VaultDecryptionBatchException(errors + " decryption errors",
                                    zip(requests, results)));
                    return zipRequestToValue(requests, results);
                });
    }

    private Uni<List<DecryptionResult>> decryptBatch(String keyName, List<DecryptionRequest> requests) {

        var params = new VaultSecretsTransitDecryptBatchParams()
                .setBatchInput(requests.stream().map(r -> new VaultSecretsTransitDecryptBatchItem()
                        .setCiphertext(r.getCiphertext())
                        .setContext(r.getContext()))
                        .collect(toList()));

        TransitKeyConfig config = getTransitConfig(keyName);

        String configKeyName;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
        } else {
            configKeyName = keyName;
        }

        return Uni.createFrom().completionStage(transit.decryptBatch(configKeyName, params))
                .map(result -> result.stream()
                        .map(r -> new DecryptionResult(new ClearData(r.getPlaintext()), r.getError()))
                        .collect(toList()));
    }

    // ---

    @Override
    public Uni<String> rewrap(String keyName, String ciphertext) {
        return rewrap(keyName, ciphertext, null);
    }

    @Override
    public Uni<String> rewrap(String keyName, String ciphertext, TransitContext transitContext) {
        RewrappingRequest item = new RewrappingRequest(ciphertext, transitContext);
        return rewrapBatch(keyName, singletonList(item)).map(results -> results.get(0).getValueOrElseError());
    }

    @Override
    public Uni<Map<RewrappingRequest, String>> rewrap(String keyName, List<RewrappingRequest> requests) {
        return rewrapBatch(keyName, requests)
                .map(results -> {
                    checkBatchErrors(results,
                            errors -> new VaultRewrappingBatchException(errors + " rewrapping errors",
                                    zip(requests, results)));
                    return zipRequestToValue(requests, results);
                });
    }

    private Uni<List<EncryptionResult>> rewrapBatch(String keyName, List<RewrappingRequest> requests) {

        var params = new VaultSecretsTransitRewrapBatchParams()
                .setBatchInput(requests.stream().map(r -> new VaultSecretsTransitRewrapBatchItem()
                        .setCiphertext(r.getCiphertext())
                        .setKeyVersion(r.getKeyVersion())
                        .setContext(r.getContext()))
                        .collect(toList()));

        TransitKeyConfig config = getTransitConfig(keyName);

        String configKeyName;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
        } else {
            configKeyName = keyName;
        }

        return Uni.createFrom().completionStage(transit.rewrapBatch(configKeyName, params))
                .map(result -> result.stream()
                        .map(r -> new EncryptionResult(r.getCiphertext(), r.getError()))
                        .collect(toList()));
    }

    // ---

    @Override
    public Uni<String> sign(String keyName, String input) {
        return sign(keyName, new SigningInput(input), null);
    }

    @Override
    public Uni<String> sign(String keyName, SigningInput input, TransitContext transitContext) {
        return sign(keyName, input, null, transitContext);
    }

    @Override
    public Uni<String> sign(String keyName, SigningInput input, SignVerifyOptions options,
            TransitContext transitContext) {
        SigningRequest item = new SigningRequest(input, transitContext);
        List<SigningRequestResultPair> pairs = singletonList(new SigningRequestResultPair(item));
        return signBatch(keyName, NO_KEY_VERSION, pairs, options)
                .map(v -> pairs.get(0).getResult().getValueOrElseError());
    }

    @Override
    public Uni<Map<SigningRequest, String>> sign(String keyName, List<SigningRequest> requests) {
        return sign(keyName, requests, null);
    }

    @Override
    public Uni<Map<SigningRequest, String>> sign(String keyName, List<SigningRequest> requests,
            SignVerifyOptions options) {
        return Multi.createFrom().iterable(requests)
                .map(SigningRequestResultPair::new)
                .group().by(SigningRequestResultPair::getKeyVersion)
                .onItem().transformToMultiAndMerge(group -> {
                    // Sign each batch of requests, which are grouped by key version. When each
                    // batch is complete, return the result from each request pair as a merged stream.
                    int keyVersion = group.key();
                    return group.collect().asList().onItem().transformToMulti(pairs -> {
                        return signBatch(keyName, keyVersion, pairs, options)
                                .onItem().transformToMulti(v -> {
                                    return Multi.createFrom().iterable(pairs).map(SigningRequestResultPair::getResult);
                                });
                    });
                })
                .collect().asList().map(results -> {
                    checkBatchErrors(results,
                            errors -> new VaultSigningBatchException(errors + " signing errors",
                                    zip(requests, results)));
                    return zipRequestToValue(requests, results);
                });
    }

    private Uni<Void> signBatch(String keyName, int keyVersion, List<SigningRequestResultPair> pairs,
            SignVerifyOptions options) {

        var params = new VaultSecretsTransitSignBatchParams()
                .setBatchInput(pairs.stream().map(pair -> {
                    SigningRequest request = pair.getRequest();
                    return new VaultSecretsTransitSignBatchItem()
                            .setInput(request.getInput().getValue())
                            .setContext(request.getContext());
                }).collect(toList()))
                .setKeyVersion(keyVersion == NO_KEY_VERSION ? null : keyVersion);

        TransitKeyConfig config = getTransitConfig(keyName);

        final String configKeyName;
        final String configHashAlgorithm;
        final String configSignatureAlgorithm;
        final Boolean configPrehashed;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
            configHashAlgorithm = config.hashAlgorithm().orElse(null);
            configSignatureAlgorithm = config.signatureAlgorithm().orElse(null);
            configPrehashed = config.prehashed().orElse(null);
        } else {
            configKeyName = keyName;
            configHashAlgorithm = null;
            configSignatureAlgorithm = null;
            configPrehashed = null;
        }

        if (options != null) {
            params.setHashAlgorithm(VaultHashAlgorithm.from(defaultIfNull(options.getHashAlgorithm(), configHashAlgorithm)));
            params.setSignatureAlgorithm(VaultSecretsTransitSignatureAlgorithm
                    .from(defaultIfNull(options.getSignatureAlgorithm(), configSignatureAlgorithm)));
            params.setPrehashed(defaultIfNull(options.getPrehashed(), configPrehashed));
            params.setMarshalingAlgorithm(
                    VaultSecretsTransitMarshalingAlgorithm.from(defaultIfNull(options.getMarshalingAlgorithm(), null)));
        }

        return Uni.createFrom().completionStage(transit.signBatch(configKeyName, params))
                .map(result -> {
                    for (int i = 0; i < pairs.size(); i++) {
                        var batchResult = result.get(i);
                        SigningRequestResultPair pair = pairs.get(i);
                        pair.setResult(new SigningResult(batchResult.getSignature(), batchResult.getError()));
                    }
                    return null;
                });
    }

    // ---

    @Override
    public Uni<Void> verifySignature(String keyName, String signature, String input) {
        return verifySignature(keyName, signature, new SigningInput(input), null);
    }

    @Override
    public Uni<Void> verifySignature(String keyName, String signature, SigningInput input,
            TransitContext transitContext) {
        return verifySignature(keyName, signature, input, null, transitContext);
    }

    @Override
    public Uni<Void> verifySignature(String keyName, String signature, SigningInput input, SignVerifyOptions options,
            TransitContext transitContext) {
        VerificationRequest item = new VerificationRequest(signature, input, transitContext);
        return verifyBatch(keyName, singletonList(item), options)
                .map(batch -> {
                    Boolean valid = batch.get(0).getValueOrElseError();
                    if (!TRUE.equals(valid)) {
                        throw new VaultException(INVALID_SIGNATURE);
                    }
                    return null;
                });
    }

    @Override
    public Uni<Void> verifySignature(String keyName, List<VerificationRequest> requests) {
        return verifySignature(keyName, requests, null);
    }

    @Override
    public Uni<Void> verifySignature(String keyName, List<VerificationRequest> requests, SignVerifyOptions options) {
        return verifyBatch(keyName, requests, options)
                .map(results -> {
                    Map<VerificationRequest, VerificationResult> resultMap = zip(requests, results);
                    checkBatchErrors(results,
                            errors -> new VaultVerificationBatchException(errors + " verification errors", resultMap));
                    return null;
                });
    }

    private Uni<List<VerificationResult>> verifyBatch(String keyName, List<VerificationRequest> requests,
            SignVerifyOptions options) {

        var params = new VaultSecretsTransitVerifyBatchParams()
                .setBatchInput(requests.stream().map(r -> new VaultSecretsTransitVerifyBatchItem()
                        .setInput(r.getInput().getValue())
                        .setContext(r.getContext())
                        .setSignature(r.getSignature()))
                        .collect(toList()));

        TransitKeyConfig config = getTransitConfig(keyName);

        final String configKeyName;
        final String configHashAlgorithm;
        final String configSignatureAlgorithm;
        final Boolean configPrehashed;
        if (config != null) {
            configKeyName = config.name().orElse(keyName);
            configHashAlgorithm = config.hashAlgorithm().orElse(null);
            configSignatureAlgorithm = config.signatureAlgorithm().orElse(null);
            configPrehashed = config.prehashed().orElse(null);
        } else {
            configKeyName = keyName;
            configHashAlgorithm = null;
            configSignatureAlgorithm = null;
            configPrehashed = null;
        }

        if (options != null) {
            params.setHashAlgorithm(VaultHashAlgorithm.from(defaultIfNull(options.getHashAlgorithm(), configHashAlgorithm)));
            params.setSignatureAlgorithm(VaultSecretsTransitSignatureAlgorithm
                    .from(defaultIfNull(options.getSignatureAlgorithm(), configSignatureAlgorithm)));
            params.setPrehashed(defaultIfNull(options.getPrehashed(), configPrehashed));
            params.setMarshalingAlgorithm(
                    VaultSecretsTransitMarshalingAlgorithm.from(defaultIfNull(options.getMarshalingAlgorithm(), null)));
        }

        return Uni.createFrom().completionStage(transit.verifyBatch(configKeyName, params))
                .map(result -> result.stream()
                        .map(r -> {
                            if (r.getError() != null) {
                                return new VerificationResult(r.isValid(), r.getError());
                            } else {
                                return new VerificationResult(r.isValid(), !r.isValid() ? INVALID_SIGNATURE : null);
                            }
                        })
                        .collect(toList()));
    }

    @Override
    public Uni<Void> createKey(String keyName, KeyCreationRequestDetail detail) {

        var params = new VaultSecretsTransitCreateKeyParams();

        if (detail != null) {
            params.setAllowPlaintextBackup(detail.getAllowPlaintextBackup())
                    .setConvergentEncryption(Boolean.valueOf(detail.getConvergentEncryption()))
                    .setDerived(detail.getDerived())
                    .setExportable(detail.getExportable())
                    .setType(VaultSecretsTransitKeyType.from(detail.getType()));
        }

        return Uni.createFrom().completionStage(transit.createKey(keyName, params)).map(r -> null);
    }

    @Override
    public Uni<Void> updateKeyConfiguration(String keyName, KeyConfigRequestDetail detail) {

        var params = new VaultSecretsTransitUpdateKeyParams()
                .setAllowPlaintextBackup(detail.getAllowPlaintextBackup())
                .setDeletionAllowed(detail.getDeletionAllowed())
                .setExportable(detail.getExportable())
                .setMinDecryptionVersion(detail.getMinDecryptionVersion())
                .setMinEncryptionVersion(detail.getMinEncryptionVersion());

        return Uni.createFrom().completionStage(transit.updateKey(keyName, params));
    }

    @Override
    public Uni<Void> rotateKey(String keyName) {
        var params = new VaultSecretsTransitRotateKeyParams();
        return Uni.createFrom().completionStage(transit.rotateKey(keyName, params)).map(r -> null);
    }

    @Override
    public Uni<Void> deleteKey(String keyName) {
        return Uni.createFrom().completionStage(transit.deleteKey(keyName));
    }

    @Override
    public Uni<VaultTransitKeyExportDetail> exportKey(String keyName, VaultTransitExportKeyType keyType,
            String keyVersion) {
        return Uni.createFrom()
                .completionStage(
                        transit.exportKey(VaultSecretsTransitExportKeyType.from(keyType.name() + "-key"), keyName, keyVersion))
                .map(result -> new VaultTransitKeyExportDetail()
                        .setName(result.getName())
                        .setKeys(result.getKeys()));
    }

    @Override
    public Uni<Optional<VaultTransitKeyDetail<?>>> readKey(String keyName) {
        Uni<Optional<VaultTransitKeyDetail<?>>> res = Uni.createFrom().completionStage(transit.readKey(keyName))
                .map(result -> Optional.of(map(result)));
        return res.plug(Plugs::notFoundToEmpty);
    }

    @Override
    public Uni<List<String>> listKeys() {
        return Uni.createFrom().completionStage(transit.listKeys());
    }

    @Override
    public Uni<VaultTransitDataKey> generateDataKey(VaultTransitDataKeyType type, String keyName,
            VaultTransitDataKeyRequestDetail detail) {

        var keyType = VaultSecretsTransitDataKeyType.from(type.name());

        var params = detail == null ? null
                : new VaultSecretsTransitGenerateDataKeyParams()
                        .setBits(detail.getBits())
                        .setNonce(detail.getNonce())
                        .setContext(detail.getContext());

        return Uni.createFrom().completionStage(transit.generateDataKey(keyType, keyName, params))
                .map(r -> new VaultTransitDataKey().setCiphertext(r.getCiphertext()).setPlaintext(r.getPlaintext()));
    }

    protected VaultTransitKeyDetail<?> map(VaultSecretsTransitKeyInfo info) {
        var latestVersionData = info.getKeys().get(Integer.toString(info.getLatestVersion()));
        VaultTransitKeyDetail<?> result;
        if (latestVersionData.getPublicKey() != null) {
            Map<String, VaultTransitAsymmetricKeyVersion> versions = info.getKeys().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, VaultTransitManager::mapAsymmetricKeyVersion));
            result = new VaultTransitAsymmetricKeyDetail()
                    .setVersions(versions);
        } else {
            Map<String, VaultTransitSymmetricKeyVersion> versions = info.getKeys().entrySet().stream()
                    .collect(Collectors.toMap(Entry::getKey, VaultTransitManager::mapSymmetricKeyVersion));
            result = new VaultTransitSymmetricKeyDetail()
                    .setVersions(versions);
        }
        result.setDeletionAllowed(info.isDeletionAllowed());
        result.setDerived(info.isDerived());
        result.setExportable(info.isExportable());
        result.setAllowPlaintextBackup(info.isAllowPlaintextBackup());
        result.setLatestVersion(info.getLatestVersion());
        result.setMinAvailableVersion(info.getMinAvailableVersion());
        result.setMinDecryptionVersion(info.getMinDecryptionVersion());
        result.setMinEncryptionVersion(info.getMinEncryptionVersion());
        result.setName(info.getName());
        result.setSupportsEncryption(info.isSupportsEncryption());
        result.setSupportsDecryption(info.isSupportsDecryption());
        result.setSupportsDerivation(info.isSupportsDerivation());
        result.setSupportsSigning(info.isSupportsSigning());
        result.setType(info.getType().getValue());
        return result;
    }

    private static VaultTransitAsymmetricKeyVersion mapAsymmetricKeyVersion(
            Map.Entry<String, VaultSecretsTransitKeyVersion> entry) {
        var value = entry.getValue();
        var version = new VaultTransitAsymmetricKeyVersion();
        version.setName(value.getName());
        version.setPublicKey(value.getPublicKey());
        version.setCreationTime(value.getCreationTime());
        return version;
    }

    private static VaultTransitSymmetricKeyVersion mapSymmetricKeyVersion(
            Map.Entry<String, VaultSecretsTransitKeyVersion> entry) {
        var value = entry.getValue();
        var version = new VaultTransitSymmetricKeyVersion();
        version.setCreationTime(value.getCreationTime());
        return version;
    }

    // ---

    private void checkBatchErrors(List<? extends VaultTransitBatchResult<?>> results,
            Function<Long, ? extends VaultException> exceptionProducer) {
        long errors = results.stream().filter(VaultTransitBatchResult::isInError).count();
        if (errors != 0) {
            throw exceptionProducer.apply(errors);
        }
    }

    private <K, V> Map<K, V> zip(List<K> keys, List<V> values) {
        return zip(keys, values, identity());
    }

    private <K, V extends VaultTransitBatchResult<T>, T> Map<K, T> zipRequestToValue(List<K> keys, List<V> values) {
        return zip(keys, values, VaultTransitBatchResult::getValue);
    }

    private <K, T, V> Map<K, V> zip(List<K> keys, List<T> values, Function<T, V> f) {
        if (keys.size() != values.size()) {
            throw new VaultException("unable to zip " + keys.size() + " keys with " + values.size() + " values");
        }
        Map<K, V> map = new IdentityHashMap<>();
        IntStream.range(0, keys.size()).forEach(i -> map.put(keys.get(i), f.apply(values.get(i))));
        return map;
    }

    private <T> T defaultIfNull(T value, T defaultValue) {
        if (value != null) {
            return value;
        }
        return defaultValue;
    }

}

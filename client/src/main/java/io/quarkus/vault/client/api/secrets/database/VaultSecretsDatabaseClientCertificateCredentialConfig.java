package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VaultSecretsDatabaseClientCertificateCredentialConfig implements VaultSecretsDatabaseCredentialConfig {

    public enum KeyType {
        @JsonProperty("rsa")
        RSA,
        @JsonProperty("ec")
        EC,
        @JsonProperty("ed25519")
        ED25519
    }

    public enum SignatureBits {
        @JsonProperty("256")
        BITS_256,
        @JsonProperty("384")
        BITS_384,
        @JsonProperty("512")
        BITS_512
    }

    @JsonProperty("common_name_template")
    private String commonNameTemplate;

    @JsonProperty("ca_cert")
    private String caCert;

    @JsonProperty("ca_private_key")
    private String caPrivateKey;

    @JsonProperty("key_type")
    private KeyType keyType;

    @JsonProperty("key_bits")
    private KeyBits keyBits;

    @JsonProperty("signature_bits")
    private SignatureBits signatureBits;

    @Override
    public VaultSecretsDatabaseCredentialType getType() {
        return VaultSecretsDatabaseCredentialType.CLIENT_CERTIFICATE;
    }

    public String getCommonNameTemplate() {
        return commonNameTemplate;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setCommonNameTemplate(String commonNameTemplate) {
        this.commonNameTemplate = commonNameTemplate;
        return this;
    }

    public String getCaCert() {
        return caCert;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setCaCert(String caCert) {
        this.caCert = caCert;
        return this;
    }

    public String getCaPrivateKey() {
        return caPrivateKey;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setCaPrivateKey(String caPrivateKey) {
        this.caPrivateKey = caPrivateKey;
        return this;
    }

    public KeyType getKeyType() {
        return keyType;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setKeyType(KeyType keyType) {
        this.keyType = keyType;
        return this;
    }

    public KeyBits getKeyBits() {
        return keyBits;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setKeyBits(KeyBits keyBits) {
        this.keyBits = keyBits;
        return this;
    }

    public SignatureBits getSignatureBits() {
        return signatureBits;
    }

    public VaultSecretsDatabaseClientCertificateCredentialConfig setSignatureBits(SignatureBits signatureBits) {
        this.signatureBits = signatureBits;
        return this;
    }
}

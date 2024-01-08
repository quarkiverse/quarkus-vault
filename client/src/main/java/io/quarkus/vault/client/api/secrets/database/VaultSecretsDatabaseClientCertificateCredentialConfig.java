package io.quarkus.vault.client.api.secrets.database;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

import io.quarkus.vault.client.common.VaultModel;

public class VaultSecretsDatabaseClientCertificateCredentialConfig implements VaultSecretsDatabaseCredentialConfig {

    public enum KeyType implements VaultModel {
        @JsonProperty("rsa")
        RSA("rsa"),
        @JsonProperty("ec")
        EC("ec"),
        @JsonProperty("ed25519")
        ED25519("ed25519");

        private final String value;

        KeyType(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue();
        }

        @JsonCreator
        public static KeyType from(String value) {
            if (value == null)
                return null;
            for (var v : values()) {
                if (v.value.equals(value))
                    return v;
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
    }

    public enum SignatureBits implements VaultModel {
        @JsonProperty("256")
        BITS_256("256"),
        @JsonProperty("384")
        BITS_384("384"),
        @JsonProperty("512")
        BITS_512("512");

        private final String value;

        SignatureBits(String value) {
            this.value = value;
        }

        @JsonValue
        public String getValue() {
            return value;
        }

        @Override
        public String toString() {
            return getValue();
        }

        @JsonCreator
        public static SignatureBits from(String value) {
            if (value == null)
                return null;
            for (var v : values()) {
                if (v.value.equals(value))
                    return v;
            }
            throw new IllegalArgumentException("Unknown value: " + value);
        }
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

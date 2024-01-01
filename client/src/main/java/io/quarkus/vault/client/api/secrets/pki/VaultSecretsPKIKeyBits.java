package io.quarkus.vault.client.api.secrets.pki;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VaultSecretsPKIKeyBits {

    RSA_2048(2048),
    RSA_3072(3072),
    RSA_4096(4096),
    RSA_8192(8192),
    EC_P224(224),
    EC_P256(256),
    EC_P384(384),
    EC_P521(521);

    private final int bits;

    @JsonValue
    public int getBits() {
        return bits;
    }

    VaultSecretsPKIKeyBits(int bits) {
        this.bits = bits;
    }

    @JsonCreator
    public static VaultSecretsPKIKeyBits fromBits(int bits) {
        for (VaultSecretsPKIKeyBits keyBits : VaultSecretsPKIKeyBits.values()) {
            if (keyBits.getBits() == bits) {
                return keyBits;
            }
        }
        throw new IllegalArgumentException("Unknown bits: " + bits);
    }

}

package io.quarkus.vault.client.api.secrets.pki;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum VaultSecretsPKISignatureBits {
    DEFAULT(0),
    SHA_256(256),
    SHA_384(384),
    SHA_512(512);

    private final int bits;

    @JsonValue
    public int getBits() {
        return bits;
    }

    VaultSecretsPKISignatureBits(int bits) {
        this.bits = bits;
    }

    @JsonCreator
    public static VaultSecretsPKISignatureBits fromBits(int bits) {
        for (VaultSecretsPKISignatureBits keyBits : VaultSecretsPKISignatureBits.values()) {
            if (keyBits.getBits() == bits) {
                return keyBits;
            }
        }
        throw new IllegalArgumentException("Unknown bits: " + bits);
    }

}

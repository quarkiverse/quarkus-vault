package io.quarkus.vault.transit;

public class VaultTransitDataKeyRequestDetail {

    private String context;

    private String nonce;

    private Integer bits;

    public String getContext() {
        return context;
    }

    public VaultTransitDataKeyRequestDetail setContext(String context) {
        this.context = context;
        return this;
    }

    public String getNonce() {
        return nonce;
    }

    public VaultTransitDataKeyRequestDetail setNonce(String nonce) {
        this.nonce = nonce;
        return this;
    }

    public Integer getBits() {
        return bits;
    }

    public VaultTransitDataKeyRequestDetail setBits(Integer bits) {
        this.bits = bits;
        return this;
    }
}

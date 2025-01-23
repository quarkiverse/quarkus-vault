package io.quarkus.vault.client.auth;

import io.quarkus.vault.client.VaultException;

public class VaultTokenException extends VaultException {

    public enum Reason {
        TOKEN_EXPIRED("Token has expired"),
        TOKEN_USES_EXHAUSTED("Token has exhausted its allowed usages"),

        ;

        public final String message;

        Reason(String message) {
            this.message = message;
        }
    }

    public final Reason reason;

    public VaultTokenException(Reason reason) {
        super(reason.message);
        this.reason = reason;
    }

}

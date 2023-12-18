package io.quarkus.vault.generator.errors;

import java.util.Optional;

public class SpecError extends RuntimeException {

    public SpecError(String message, Throwable cause) {
        super(message, cause);
    }

    public static Optional<SpecError> unwrap(Throwable throwable) {
        if (throwable instanceof SpecError specError) {
            return Optional.of(specError);
        } else if (throwable instanceof RuntimeException e) {
            return unwrap(e.getCause());
        }
        return Optional.empty();
    }

    public void print(String indent) {
        System.err.println(indent + getMessage());
        if (getCause() instanceof SpecError specError) {
            specError.print(indent + "  ");
        } else if (getCause() instanceof Exception cause) {
            System.err.println(indent + "  " + cause.getMessage());
        }
    }
}

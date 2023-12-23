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

    @Override
    public String getMessage() {
        return toString();
    }

    @Override
    public String toString() {
        return toString("");
    }

    public String toString(String indent) {
        var sb = new StringBuilder();
        sb.append(indent).append(super.getMessage());
        if (getCause() instanceof SpecError specError) {
            sb.append("\n").append(specError.toString(indent + "  "));
        } else if (getCause() instanceof Exception cause) {
            sb.append("\n").append(indent).append("  ").append(cause.getMessage());
        }
        return sb.toString();
    }

    public SpecError withoutCause() {
        return new SpecError(getMessage(), null);
    }
}

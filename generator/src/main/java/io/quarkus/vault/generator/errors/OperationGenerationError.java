package io.quarkus.vault.generator.errors;

public class OperationGenerationError extends SpecError {

    public final String operationName;

    public OperationGenerationError(String operationName, String message, Throwable cause) {
        super(message, cause);
        this.operationName = operationName;
    }

    public static OperationGenerationError of(String operationName, Throwable cause) {
        return new OperationGenerationError(operationName, "Error generating operation: " + operationName, cause);
    }
}

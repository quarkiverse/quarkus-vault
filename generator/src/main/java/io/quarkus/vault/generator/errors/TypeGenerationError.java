package io.quarkus.vault.generator.errors;

public class TypeGenerationError extends SpecError {

    public final String typeName;

    public TypeGenerationError(String typeName, String message, Throwable cause) {
        super(message, cause);
        this.typeName = typeName;
    }

    public static TypeGenerationError of(String typeName, Throwable cause) {
        return new TypeGenerationError(typeName, "Error generating POJO type from spec: " + typeName, cause);
    }
}

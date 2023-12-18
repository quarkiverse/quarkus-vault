package io.quarkus.vault.generator.errors;

public class DisallowedParameterError extends SpecError {

    public final String parameterName;

    public DisallowedParameterError(String parameterName, String message) {
        super(message, null);
        this.parameterName = parameterName;
    }

    public static DisallowedParameterError of(String parameterName) {
        return new DisallowedParameterError(parameterName, "Parameter '" + parameterName + "' is not allowed in this context");
    }
}

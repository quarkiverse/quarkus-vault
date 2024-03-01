package io.quarkus.vault.generator.errors;

public class MissingParameterError extends SpecError {

    public final String parameterName;

    public MissingParameterError(String parameterName, String message) {
        super(message, null);
        this.parameterName = parameterName;
    }

    public static MissingParameterError of(String parameterName) {
        return new MissingParameterError(parameterName, "Missing parameter: " + parameterName);
    }
}

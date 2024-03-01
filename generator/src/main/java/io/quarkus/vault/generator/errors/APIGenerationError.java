package io.quarkus.vault.generator.errors;

public class APIGenerationError extends SpecError {

    public final String apiName;

    public APIGenerationError(String apiName, String message, Throwable cause) {
        super(message, cause);
        this.apiName = apiName;
    }

    public static APIGenerationError of(String apiName, Throwable cause) {
        return new APIGenerationError(apiName, "Error generating API from spec: " + apiName, cause);
    }
}

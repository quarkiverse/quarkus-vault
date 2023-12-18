package io.quarkus.vault.generator.errors;

import java.util.Arrays;
import java.util.stream.Collectors;

public class OneOfFieldsMissingError extends SpecError {

    public final String[] fields;

    public OneOfFieldsMissingError(String message, String... fields) {
        super(message, null);
        this.fields = fields;
    }

    public static OneOfFieldsMissingError of(String message, String... fields) {
        var fieldsMsg = Arrays.stream(fields).map(f -> "'" + f + "'").collect(Collectors.joining(", "));
        return new OneOfFieldsMissingError(message + ": One of " + fieldsMsg + " required", fields);
    }
}

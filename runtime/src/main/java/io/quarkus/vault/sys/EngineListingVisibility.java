package io.quarkus.vault.sys;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EngineListingVisibility {
    UNAUTH("unauth"),
    HIDDEN("hidden");

    private final String value;

    EngineListingVisibility(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return getValue();
    }

    @JsonCreator
    public static EngineListingVisibility from(String value) {
        if (value == null) {
            return null;
        }
        for (EngineListingVisibility visibility : values()) {
            if (visibility.value.equals(value)) {
                return visibility;
            }
        }
        throw new IllegalArgumentException("Unknown engine listing visibility: " + value);
    }
}

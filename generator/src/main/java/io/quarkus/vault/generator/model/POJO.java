package io.quarkus.vault.generator.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.generator.utils.Strings;

public record POJO(
        @JsonProperty(required = true) String name,
        @JsonProperty("extends") Optional<String> extendsName,
        @JsonProperty("implements") Optional<List<String>> implementsNames,
        Optional<List<POJO>> nested,
        Optional<List<Property>> properties,
        Optional<List<Method>> methods) implements AnyPOJO {

    public record Property(
            @JsonProperty(required = true) String name,
            Optional<String> serializedName,
            Optional<Boolean> required,
            Optional<String> type) {

        public String getSerializedName() {
            return serializedName.orElseGet(() -> Strings.camelCaseToSnakeCase(name));
        }

        public boolean isRequired() {
            return required.orElse(false);
        }

        public String getImpliedType() {
            return type.orElse("java.lang.String");
        }

    }

    public record Method(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String returnType,
            Optional<Map<String, String>> parameters,
            @JsonProperty(required = true) String body) {
    }

}

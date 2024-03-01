package io.quarkus.vault.generator.model;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.vault.generator.utils.Strings;

public record POJO(
        @JsonProperty(required = true) String name,
        @JsonProperty("extends") Optional<String> extendsName,
        @JsonProperty("implements") Optional<List<String>> implementNames,
        Optional<List<POJO.Annotation>> annotations,
        Optional<List<POJO>> nested,
        Optional<List<Property>> properties,
        Optional<List<Method>> methods) implements AnyPOJO {

    public record Property(
            @JsonProperty(required = true) String name,
            Optional<String> serializedName,
            Optional<Boolean> required,
            Optional<String> type,
            Optional<List<POJO.Property>> object,
            Optional<List<Annotation>> annotations) {

        public String getSerializedName() {
            return serializedName.orElseGet(() -> Strings.camelCaseToSnakeCase(name));
        }

        public boolean isRequired() {
            return required.orElse(false);
        }

    }

    public record Method(
            @JsonProperty(required = true) String name,
            @JsonProperty(required = true) String returnType,
            Optional<List<String>> typeParameters,
            Optional<Map<String, String>> parameters,
            @JsonProperty(required = true) String body,
            Optional<Map<String, String>> bodyArguments,
            Optional<List<Annotation>> annotations) {
    }

    public record Annotation(
            @JsonProperty(required = true) String type,
            Optional<Map<String, Member>> members) {

        public record Member(
                @JsonProperty(required = true) String format,
                Optional<Map<String, String>> arguments) {
        }
    }

}

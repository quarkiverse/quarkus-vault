package io.quarkus.vault.generator.model;

import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PartialPOJO(
        @JsonProperty("implements") Optional<List<String>> implementsNames,
        Optional<List<POJO.Property>> properties,
        Optional<List<POJO.Method>> methods) implements AnyPOJO {

    public static PartialPOJO of(List<POJO.Property> properties) {
        return new PartialPOJO(Optional.empty(), Optional.of(properties), Optional.empty());
    }

    @Override
    public Optional<String> extendsName() {
        return Optional.empty();
    }
}

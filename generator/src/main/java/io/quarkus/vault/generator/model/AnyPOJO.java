package io.quarkus.vault.generator.model;

import java.util.List;
import java.util.Optional;

public interface AnyPOJO {

    Optional<String> extendsName();

    Optional<List<String>> implementsNames();

    Optional<List<POJO>> nested();

    Optional<List<POJO.Property>> properties();

    Optional<List<POJO.Method>> methods();

    static AnyPOJO empty() {
        return PartialPOJO.of(List.of());
    }

}

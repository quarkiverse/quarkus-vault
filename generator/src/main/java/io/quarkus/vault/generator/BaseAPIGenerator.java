package io.quarkus.vault.generator;

import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.utils.TypeNames;

public class BaseAPIGenerator extends BaseGenerator {

    protected final API api;
    private final TypeNames typeNames;

    protected BaseAPIGenerator(API api) {
        this.api = api;
        this.typeNames = new TypeNames(api);
    }

    @Override
    protected TypeNames getTypeNames() {
        return typeNames;
    }

}

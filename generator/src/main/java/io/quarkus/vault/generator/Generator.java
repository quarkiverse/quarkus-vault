package io.quarkus.vault.generator;

import java.util.Map;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeSpec;

public interface Generator {

    Map<ClassName, TypeSpec> getGeneratedTypes();

}

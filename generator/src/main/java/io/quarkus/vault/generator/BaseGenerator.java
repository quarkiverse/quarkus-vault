package io.quarkus.vault.generator;

import static io.quarkus.vault.generator.utils.Strings.capitalize;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import jakarta.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.squareup.javapoet.*;

import io.quarkus.vault.generator.model.AnyPOJO;
import io.quarkus.vault.generator.model.POJO;
import io.quarkus.vault.generator.utils.TypeNames;

public abstract class BaseGenerator implements Generator {

    private final Map<ClassName, TypeSpec> generatedTypes = new LinkedHashMap<>();

    abstract protected TypeNames getTypeNames();

    @Override
    public Map<ClassName, TypeSpec> getGeneratedTypes() {
        return generatedTypes;
    }

    protected void addGeneratedType(ClassName name, Function<String, TypeSpec> generator) {
        generatedTypes.computeIfAbsent(name, className -> generator.apply(className.simpleName()));
    }

    public ClassName typeNameFor(String... suffixes) {
        return getTypeNames().typeNameFor(suffixes);
    }

    public ClassName className(String packageName, String name) {
        return getTypeNames().className(packageName, name);
    }

    public ClassName className(Class<?> clazz) {
        return getTypeNames().className(clazz);
    }

    public TypeName typeName(String name) {
        return getTypeNames().typeName(name);
    }

    public TypeName typeName(ClassName name, TypeName... parameterTypes) {
        return getTypeNames().typeName(name, parameterTypes);
    }

    public TypeSpec generatePOJO(String name, AnyPOJO pojo) {
        return generatePOJO(name, pojo, spec -> {
        });
    }

    public TypeSpec generatePOJO(String name, AnyPOJO pojo, Consumer<TypeSpec.Builder> customizer) {
        var spec = startPOJO(name, customizer);
        var specName = getTypeNames().typeName(spec.build());
        if (pojo.extendsName().isPresent()) {
            spec.superclass(typeName(pojo.extendsName().get()));
        }
        if (pojo.implementsNames().isPresent()) {
            for (var iface : pojo.implementsNames().get()) {
                spec.addSuperinterface(typeName(iface));
            }
        }
        if (pojo.properties().isPresent()) {
            addPOJOProperties(specName, spec, pojo.properties().get());
        }
        if (pojo.methods().isPresent()) {
            addPOJOMethods(spec, pojo.methods().get());
        }
        return spec.build();
    }

    public TypeSpec.Builder startPOJO(String name, Consumer<TypeSpec.Builder> customizer) {
        var spec = getTypeNames().classSpecBuilder(name)
                .addModifiers(Modifier.PUBLIC);
        customizer.accept(spec);
        return spec;
    }

    public void addPOJOProperties(TypeName specName, TypeSpec.Builder spec, List<POJO.Property> properties) {

        for (var property : properties) {
            spec.addField(generatePOJOField(property));
        }

        for (var property : properties) {
            spec.addMethod(generatePOJOSetter(specName, property));
        }
    }

    public FieldSpec generatePOJOField(POJO.Property property) {
        var spec = FieldSpec.builder(typeName(property.getImpliedType()), property.name())
                .addModifiers(Modifier.PUBLIC);

        var serializedName = property.getSerializedName();
        if (!Objects.equals(serializedName, property.name())) {

            spec.addAnnotation(AnnotationSpec.builder(className(JsonProperty.class))
                    .addMember("value", "$S", serializedName)
                    .build());
        }

        return spec.build();
    }

    public MethodSpec generatePOJOSetter(TypeName specName, POJO.Property property) {
        var parameterSpec = ParameterSpec.builder(typeName(property.getImpliedType()), property.name());
        if (!property.isRequired()) {
            parameterSpec.addAnnotation(className(Nonnull.class));
        }
        var spec = MethodSpec.methodBuilder("set" + capitalize(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName(property.getImpliedType()), property.name())
                .returns(specName)
                .addStatement("this.$L = $L", property.name(), property.name())
                .addStatement("return this");
        return spec.build();
    }

    public void addPOJOMethods(TypeSpec.Builder spec, List<POJO.Method> methods) {
        for (var method : methods) {
            spec.addMethod(generatePOJOMethod(method));
        }
    }

    public MethodSpec generatePOJOMethod(POJO.Method method) {

        var body = CodeBlock.builder()
                .indent()
                .add(method.body())
                .unindent()
                .build();

        var spec = MethodSpec.methodBuilder(method.name())
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName(method.returnType()))
                .addCode(body);

        if (method.parameters().isPresent()) {
            for (var parameter : method.parameters().get().entrySet()) {
                spec.addParameter(typeName(parameter.getValue()), parameter.getKey());
            }
        }

        return spec.build();
    }

}

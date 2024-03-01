package io.quarkus.vault.generator;

import static io.quarkus.vault.generator.utils.Strings.capitalize;
import static io.quarkus.vault.generator.utils.Strings.kebabCaseToSnakeCase;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.lang.model.element.Modifier;

import jakarta.annotation.Nonnull;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.squareup.javapoet.*;

import io.quarkus.vault.generator.errors.OneOfFieldsMissingError;
import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.model.AnyPOJO;
import io.quarkus.vault.generator.model.POJO;
import io.quarkus.vault.generator.model.PartialPOJO;
import io.quarkus.vault.generator.utils.TypeNames;

public abstract class BaseGenerator implements Generator {

    private final Map<ClassName, TypeSpec> generatedTypes = new LinkedHashMap<>();

    abstract protected TypeNames getTypeNames();

    @Override
    public Map<ClassName, TypeSpec> getGeneratedTypes() {
        return generatedTypes;
    }

    protected void addGeneratedType(ClassName name, Function<String, TypeSpec> generator) {
        var type = generatedTypes.get(name);
        if (type != null) {
            return;
        }

        var typeSpec = generator.apply(name.simpleName());
        generatedTypes.put(name, typeSpec);
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

    public ClassName className(String name) {
        return getTypeNames().className(name);
    }

    public TypeName typeName(String name) {
        return getTypeNames().typeName(name);
    }

    public TypeName typeName(ClassName name, TypeName... parameterTypes) {
        return getTypeNames().typeName(name, parameterTypes);
    }

    public Map<String, ?> codeArguments(Map<String, String> arguments) {
        return arguments.entrySet().stream().map(entry -> {
            var key = entry.getKey();
            var value = entry.getValue();
            if (value.startsWith("<type>")) {
                return Map.entry(key, typeName(value.substring("<type>".length())));
            } else {
                return Map.entry(key, value);
            }
        }).collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);
    }

    public TypeSpec generatePOJO(String name, AnyPOJO pojo, String generationPrefix) {
        return generatePOJO(name, pojo, generationPrefix, spec -> {
        });
    }

    public TypeSpec generatePOJO(String name, AnyPOJO pojo, String generationPrefix, Consumer<TypeSpec.Builder> customizer) {
        var spec = startPOJO(name, customizer);
        var specName = getTypeNames().typeName(spec.build());
        generatePOJO(spec, specName, pojo, generationPrefix);
        return spec.build();
    }

    public void generatePOJO(TypeSpec.Builder spec, TypeName specName, AnyPOJO pojo, String generationPrefix) {
        pojo.extendsName().ifPresent(extendsName -> spec.superclass(typeName(extendsName)));
        pojo.implementNames().ifPresent(implementNames -> {
            for (var implementName : implementNames) {
                spec.addSuperinterface(typeName(implementName));
            }
        });
        pojo.annotations().ifPresent(annotations -> addAnnotations(spec, annotations));
        pojo.nested().ifPresent(nested -> addNestedPOJOs(spec, specName, nested));
        pojo.properties().ifPresent(properties -> addPOJOProperties(specName, spec, properties, generationPrefix));
        pojo.methods().ifPresent(methods -> addPOJOMethods(spec, methods));
    }

    public TypeSpec.Builder startPOJO(String name, Consumer<TypeSpec.Builder> customizer) {
        var spec = getTypeNames().classSpecBuilder(name)
                .addModifiers(Modifier.PUBLIC)
                .addSuperinterface(className(getTypeNames().api().getCommonPackageName(), "VaultModel"));
        customizer.accept(spec);
        return spec;
    }

    public void addNestedPOJOs(TypeSpec.Builder spec, TypeName specName, List<POJO> nested) {
        ClassName specClassName;
        if (specName instanceof ClassName) {
            specClassName = (ClassName) specName;
        } else if (specName instanceof ParameterizedTypeName parameterizedTypeName) {
            specClassName = parameterizedTypeName.rawType;
        } else {
            throw new IllegalArgumentException("Unsupported specName type: " + specName.getClass());
        }

        for (var pojo : nested) {
            var nestedSpec = startPOJO(pojo.name(), s -> s.addModifiers(Modifier.STATIC));
            var nestedSpecName = specClassName.nestedClass(pojo.name());
            generatePOJO(nestedSpec, nestedSpecName, pojo, "");
            spec.addType(nestedSpec.build());
        }
    }

    public void addPOJOProperties(TypeName specName, TypeSpec.Builder spec, List<POJO.Property> properties,
            String generationPrefix) {

        for (var property : properties) {
            spec.addField(generatePOJOField(property, generationPrefix));
        }

        for (var property : properties) {
            spec.addMethod(generatePOJOGetter(property, generationPrefix));
            spec.addMethod(generatePOJOSetter(specName, property, generationPrefix));
        }
    }

    public FieldSpec generatePOJOField(POJO.Property property, String generationPrefix) {
        TypeName typeName;
        if (property.type().isPresent()) {

            typeName = typeName(property.type().get());

        } else if (property.object().isPresent()) {

            var clsName = typeNameFor(generationPrefix, property.name());

            addGeneratedType(clsName, className -> generatePOJO(className, PartialPOJO.of(property.object().get()), ""));

            typeName = clsName;

        } else {
            throw OneOfFieldsMissingError.of("No type specified for property " + property.name());
        }

        var spec = FieldSpec.builder(typeName, property.name())
                .addModifiers(Modifier.PRIVATE);

        var serializedName = property.getSerializedName();
        if (!Objects.equals(serializedName, property.name())) {

            spec.addAnnotation(AnnotationSpec.builder(className(JsonProperty.class))
                    .addMember("value", "$S", serializedName)
                    .build());
        }

        property.annotations().ifPresent(annotations -> addAnnotations(spec, annotations));

        return spec.build();
    }

    public MethodSpec generatePOJOGetter(POJO.Property property, String generationPrefix) {

        TypeName typeName;
        if (property.type().isPresent()) {

            typeName = typeName(property.type().get());

        } else if (property.object().isPresent()) {

            typeName = typeNameFor(capitalize(generationPrefix) + capitalize(property.name()));

        } else {
            throw OneOfFieldsMissingError.of("No type specified for property " + property.name());
        }

        String prefix;
        String name;
        if (typeName.equals(ClassName.bestGuess("Boolean")) || typeName.equals(TypeName.BOOLEAN)
                || typeName.equals(TypeName.BOOLEAN.box())) {
            prefix = "is";
            if (property.name().startsWith("is")) {
                name = property.name().substring("is".length());
            } else {
                name = property.name();
            }
        } else {
            prefix = "get";
            name = property.name();
        }

        return MethodSpec.methodBuilder(prefix + capitalize(name))
                .addModifiers(Modifier.PUBLIC)
                .returns(typeName)
                .addStatement("return this.$L", property.name())
                .build();
    }

    public MethodSpec generatePOJOSetter(TypeName specName, POJO.Property property, String generationPrefix) {

        TypeName typeName;
        if (property.type().isPresent()) {

            typeName = typeName(property.type().get());

        } else if (property.object().isPresent()) {

            typeName = typeNameFor(capitalize(generationPrefix) + capitalize(property.name()));

        } else {
            throw OneOfFieldsMissingError.of("No type specified for property " + property.name());
        }

        var parameterSpec = ParameterSpec.builder(typeName, property.name());
        if (!property.isRequired()) {
            parameterSpec.addAnnotation(className(Nonnull.class));
        }
        var spec = MethodSpec.methodBuilder("set" + capitalize(property.name()))
                .addModifiers(Modifier.PUBLIC)
                .addParameter(typeName, property.name())
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

    public void addAnnotations(TypeSpec.Builder spec, List<POJO.Annotation> annotations) {
        for (var annotation : annotations) {
            spec.addAnnotation(generateAnnotationSpec(annotation));
        }
    }

    public void addAnnotations(FieldSpec.Builder spec, List<POJO.Annotation> annotations) {
        for (var annotation : annotations) {
            spec.addAnnotation(generateAnnotationSpec(annotation));
        }
    }

    private AnnotationSpec generateAnnotationSpec(POJO.Annotation annotation) {
        var spec = AnnotationSpec.builder(className(annotation.type()));

        if (annotation.members().isPresent()) {

            for (var member : annotation.members().get().entrySet()) {

                var memberName = member.getKey();
                var memberValue = member.getValue();
                var memberFormat = memberValue.format();
                var memberArguments = codeArguments(memberValue.arguments().orElse(Map.of()));

                spec.addMember(memberName, CodeBlock.builder().addNamed(memberFormat, memberArguments).build());
            }
        }

        return spec.build();
    }

    public MethodSpec generatePOJOMethod(POJO.Method method) {

        var bodyArgs = codeArguments(method.bodyArguments().orElse(Map.of()));

        var body = CodeBlock.builder()
                .indent()
                .addNamed(method.body(), bodyArgs)
                .unindent()
                .build();

        MethodSpec.Builder spec;
        if (method.name().equalsIgnoreCase("constructor")) {
            spec = MethodSpec.constructorBuilder();
        } else {
            spec = MethodSpec.methodBuilder(method.name())
                    .returns(typeName(method.returnType()));
        }

        spec.addModifiers(Modifier.PUBLIC)
                .addCode(body);

        method.typeParameters().ifPresent(typeParameters -> {
            for (var typeParameter : typeParameters) {
                spec.addTypeVariable(getTypeNames().typeVariableName(typeParameter));
            }
        });

        method.parameters().ifPresent(parameters -> {
            for (var parameter : method.parameters().get().entrySet()) {
                spec.addParameter(typeName(parameter.getValue()), parameter.getKey());
            }
        });

        method.annotations().ifPresent(annotations -> {
            for (var annotation : annotations) {
                spec.addAnnotation(generateAnnotationSpec(annotation));
            }
        });

        return spec.build();
    }

    public void generateEnum(API.Enum e) {
        var typeName = typeNameFor(capitalize(e.name()));
        addGeneratedType(typeName, className -> {
            var spec = TypeSpec.enumBuilder(className)
                    .addModifiers(Modifier.PUBLIC)
                    .addSuperinterface(className(getTypeNames().api().getCommonPackageName(), "VaultModel"));
            for (var value : e.values()) {
                var valueName = kebabCaseToSnakeCase(value).toUpperCase();
                var valueSpec = TypeSpec.anonymousClassBuilder("$S", value)
                        .addAnnotation(AnnotationSpec.builder(JsonProperty.class)
                                .addMember("value", "$S", value)
                                .build())
                        .build();
                spec.addEnumConstant(valueName, valueSpec);
            }
            spec.addField(String.class, "value", Modifier.PRIVATE, Modifier.FINAL);
            spec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addParameter(String.class, "value")
                            .addStatement("this.value = value")
                            .build());
            spec.addMethod(
                    MethodSpec.methodBuilder("getValue")
                            .addAnnotation(JsonValue.class)
                            .addModifiers(Modifier.PUBLIC)
                            .returns(String.class)
                            .addStatement("return value")
                            .build());
            spec.addMethod(
                    MethodSpec.methodBuilder("toString")
                            .addModifiers(Modifier.PUBLIC)
                            .returns(String.class)
                            .addAnnotation(Override.class)
                            .addStatement("return getValue()")
                            .build());
            spec.addMethod(
                    MethodSpec.methodBuilder("from")
                            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                            .addAnnotation(JsonCreator.class)
                            .addParameter(String.class, "value")
                            .returns(typeName)
                            .addCode(
                                    CodeBlock.builder()
                                            .addStatement("if (value == null) return null")
                                            .beginControlFlow("for (var v : values())")
                                            .addStatement("if (v.value.equals(value)) return v")
                                            .endControlFlow()
                                            .addStatement("throw new $T(\"Unknown value: \" + value)",
                                                    IllegalArgumentException.class)
                                            .build())
                            .build());
            return spec.build();
        });
    }

}

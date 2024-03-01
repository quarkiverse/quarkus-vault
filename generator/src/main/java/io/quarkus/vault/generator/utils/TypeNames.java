package io.quarkus.vault.generator.utils;

import static io.quarkus.vault.generator.utils.Strings.capitalize;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.type.*;
import com.squareup.javapoet.*;

import io.quarkus.vault.generator.model.API;

public record TypeNames(API api) {

    public String typeSimpleNameFor(String... suffixes) {
        return capitalize(api.getPrefix())
                + capitalize(api.getCategory()) + capitalize(api.getName())
                + Arrays.stream(suffixes).map(Strings::capitalize).collect(Collectors.joining());
    }

    public ClassName typeNameFor(String... suffixes) {
        return ClassName.get(api.getLocalAPIPackageName(), typeSimpleNameFor(suffixes));
    }

    /**
     * Creates a ClassName based on the given name.
     * <p>
     * The following prefixes are supported:
     * <ul>
     * <li>
     * <code>$</code> - the name is prefixed as if it was generated locally and targets the local package.
     * </li>
     * <li>
     * <code>$$</code> - the name is unchanged but the type targets the base package.
     * </li>
     * </ul>
     *
     * @param name the name of the class
     * @return the ClassName object
     */
    public ClassName className(String name) {
        if (name.startsWith("$$")) {
            name = name.substring(2);
            var classNameStart = name.lastIndexOf(".");
            String relativePackage;
            String className;
            if (classNameStart == -1) {
                relativePackage = "";
                className = name;
            } else {
                relativePackage = name.substring(0, classNameStart);
                if (relativePackage.startsWith(".")) {
                    relativePackage = relativePackage.substring(1);
                }
                className = name.substring(classNameStart + 1);
            }
            return className(api.getPackageName() + "." + relativePackage, className);
        }
        if (name.startsWith("$")) {
            name = name.substring(1);
            return className(api.getLocalAPIPackageName(), typeSimpleNameFor(name));
        }
        return ClassName.bestGuess(name);
    }

    public ClassName className(String packageName, String name) {
        return ClassName.get(packageName, name);
    }

    public ClassName className(Class<?> clazz) {
        return ClassName.get(clazz);
    }

    public TypeName typeName(String name, String... parameterTypes) {
        return typeName(className(name), parameterTypes);
    }

    public TypeName typeName(ClassName name, String... parameterTypes) {
        return typeName(name, Arrays.stream(parameterTypes).map(this::typeName).toArray(TypeName[]::new));
    }

    public TypeName typeName(ClassName name, TypeName... parameterTypes) {
        return ParameterizedTypeName.get(name, parameterTypes);
    }

    public TypeSpec.Builder classSpecBuilder(String signature) {

        // Handle non-generic types
        if (!signature.contains("<")) {
            return TypeSpec.classBuilder(signature);
        }

        // Create a fake Java class leveraging the input class signature
        var fakeClass = "class " + signature + "{ }";
        var cu = StaticJavaParser.parse(fakeClass);
        var classDec = (ClassOrInterfaceDeclaration) cu.getType(0);

        var typeParameters = classDec.getTypeParameters();
        var typeVariableNames = new ArrayList<TypeVariableName>();

        for (TypeParameter typeParameter : typeParameters) {
            typeVariableNames.add(typeVariableName(typeParameter));
        }

        // Create and return TypeSpec.Builder
        return TypeSpec.classBuilder(classDec.getNameAsString())
                .addTypeVariables(typeVariableNames);
    }

    public TypeVariableName typeVariableName(String name) {

        String fakeClass = "public class FakeClass<" + name + ">  {}";

        var cu = StaticJavaParser.parse(fakeClass);
        var clazz = cu.getClassByName("FakeClass").orElseThrow();
        var typeParameter = clazz.getTypeParameters().get(0);
        return typeVariableName(typeParameter);
    }

    public TypeVariableName typeVariableName(TypeParameter typeParameter) {
        var bounds = typeParameter.getTypeBound().stream().flatMap(this::typeNames).toArray(TypeName[]::new);
        return TypeVariableName.get(typeParameter.getNameAsString(), bounds);
    }

    public Stream<TypeName> typeNames(Type type) {
        if (type instanceof IntersectionType intersectionType) {
            // Handle intersection types
            return intersectionType.getElements().stream().map(this::typeName);
        } else {
            // Handle other types
            return Stream.of(typeName(type));
        }
    }

    public TypeName typeName(TypeSpec spec) {
        var rawType = ClassName.get(api.getLocalAPIPackageName(), spec.name);
        if (spec.typeVariables.isEmpty()) {
            return rawType;
        }
        return ParameterizedTypeName.get(rawType,
                spec.typeVariables.stream().map(v -> TypeVariableName.get(v.name)).toArray(TypeVariableName[]::new));
    }

    public Optional<TypeName> getPrimitive(String type) {
        return Optional.ofNullable(
                switch (type) {
                    case "void" -> TypeName.VOID;
                    case "boolean" -> TypeName.BOOLEAN;
                    case "byte" -> TypeName.BYTE;
                    case "short" -> TypeName.SHORT;
                    case "int" -> TypeName.INT;
                    case "long" -> TypeName.LONG;
                    case "char" -> TypeName.CHAR;
                    case "float" -> TypeName.FLOAT;
                    case "double" -> TypeName.DOUBLE;
                    default -> null;
                });
    }

    public TypeName typeName(String genericType) {

        // Wrap the input within a fake class to create a valid Java file that JavaParser can parse
        String fakeClass = "public class FakeClass { " + genericType + " fakeField; }";

        CompilationUnit cu = StaticJavaParser.parse(fakeClass);
        ClassOrInterfaceDeclaration clazz = cu.getClassByName("FakeClass").orElseThrow();
        FieldDeclaration field = clazz.getFieldByName("fakeField").orElseThrow();
        return typeName(field.getVariable(0).getType());
    }

    private TypeName typeName(Type type) {
        if (type instanceof ClassOrInterfaceType classType) {
            String typeName = classType.getNameWithScope();

            if (classType.getTypeArguments().isEmpty()) {
                // Non-generic type
                return className(typeName);
            } else {
                // Generic type
                List<Type> typeArguments = classType.getTypeArguments().get();
                TypeName[] typeNames = new TypeName[typeArguments.size()];
                for (int i = 0; i < typeArguments.size(); i++) {
                    typeNames[i] = typeName(typeArguments.get(i));
                }
                return ParameterizedTypeName.get(className(typeName), typeNames);
            }

        } else if (type instanceof WildcardType wildcardType) {

            if (wildcardType.getSuperType().isPresent()) {
                // lower-bounds wildcard
                return WildcardTypeName.supertypeOf(typeName(wildcardType.getSuperType().get()));
            } else if (wildcardType.getExtendedType().isPresent()) {
                // upper-bounds wildcard
                return WildcardTypeName.subtypeOf(typeName(wildcardType.getExtendedType().get()));
            } else {
                // Unbounded wildcard
                return WildcardTypeName.subtypeOf(Object.class);
            }

        } else if (type instanceof PrimitiveType) {
            // Simple primitive type
            return switch (type.asString()) {
                case "byte" -> TypeName.BYTE;
                case "short" -> TypeName.SHORT;
                case "int" -> TypeName.INT;
                case "long" -> TypeName.LONG;
                case "char" -> TypeName.CHAR;
                case "float" -> TypeName.FLOAT;
                case "double" -> TypeName.DOUBLE;
                case "boolean" -> TypeName.BOOLEAN;
                default -> throw new RuntimeException("Unsupported primitive type: " + type.asString());
            };

        } else if (type instanceof ArrayType arrayType) {
            // Handle arrays
            TypeName componentTypeName = typeName(arrayType.getComponentType());
            return ArrayTypeName.of(componentTypeName);

        } else {
            throw new RuntimeException("Unsupported type: " + type.getClass().getSimpleName());
        }
    }
}

package io.quarkus.vault.generator;

import java.util.List;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import jakarta.annotation.Nullable;

import com.squareup.javapoet.*;

import io.quarkus.vault.generator.errors.APIGenerationError;
import io.quarkus.vault.generator.errors.OperationGenerationError;
import io.quarkus.vault.generator.errors.SpecError;
import io.quarkus.vault.generator.errors.TypeGenerationError;
import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.model.Operation;
import io.quarkus.vault.generator.model.POJO;
import io.quarkus.vault.generator.model.PartialPOJO;

public class APIGenerator extends BaseAPIGenerator {

    public interface Contract extends Generator {
        String getName();

        ClassName typeName();

        TypeSpec.Builder start();

        TypeName operationReturnTypeName(TypeName type);

        TypeName operationResultTypeName(Operation operation);

        CodeBlock operationBody(Operation operation);

        boolean operationNeedsExplicitMountPath(Operation operation);

        default JavaFile.Builder customizeFile(TypeSpec typeSpec, JavaFile.Builder builder) {
            return builder;
        }

    }

    private final Contract contract;

    public APIGenerator(API api, Contract contract) {
        super(api);
        this.contract = contract;
    }

    public Stream<JavaFile> generate() {

        try {
            generateTypes();
        } catch (SpecError e) {
            throw APIGenerationError.of(api.name().orElse("unnamed"), e);
        }

        return generateFiles();
    }

    private void generateTypes() {
        generatePOJOTypes();
        generateEnumTypes();
        generateAPI();
    }

    private void generatePOJOTypes() {

        for (var type : api.getTypes()) {
            generatePOJOType(type);
        }
    }

    private void generateEnumTypes() {

        for (var e : api.enums().orElse(List.of())) {
            generateEnum(e);
        }
    }

    private void generatePOJOType(POJO type) {
        try {

            var typeName = typeNameFor(type.name());

            addGeneratedType(typeName, (name) -> generatePOJO(name, type, ""));
        } catch (SpecError e) {
            throw TypeGenerationError.of("POJO", e);
        }
    }

    private void generateAPI() {

        if (api.operations().isEmpty()) {
            return;
        }

        var spec = contract.start();

        for (var operation : api.operations().orElse(List.of())) {
            spec.addMethod(generateOperation(operation));
        }

        if (contract instanceof APIContract) {
            addPOJOMethods(spec, api.methods().orElse(List.of()));
        }

        addGeneratedType(contract.typeName(), (name) -> spec.build());
    }

    private Stream<JavaFile> generateFiles() {

        var contractGeneratedTypes = contract.getGeneratedTypes().entrySet().stream()
                .map(entry -> {
                    var typeName = entry.getKey();
                    var typeSpec = entry.getValue();

                    var javaFile = JavaFile.builder(typeName.packageName(), typeSpec);
                    javaFile.skipJavaLangImports(true);
                    return contract.customizeFile(typeSpec, javaFile).build();
                });

        var generatedTypes = getGeneratedTypes().entrySet().stream()
                .map(entry -> {
                    var typeName = entry.getKey();
                    var typeSpec = entry.getValue();

                    var javaFile = JavaFile.builder(typeName.packageName(), typeSpec);
                    javaFile.skipJavaLangImports(true);
                    return contract.customizeFile(typeSpec, javaFile).build();
                });

        return Stream.concat(contractGeneratedTypes, generatedTypes);
    }

    MethodSpec generateOperation(Operation operation) {
        try {
            var resultType = contract.operationResultTypeName(operation);
            var spec = MethodSpec.methodBuilder(operation.name())
                    .addModifiers(Modifier.PUBLIC)
                    .returns(contract.operationReturnTypeName(resultType))
                    .addParameters(generateRequestFactoryOperationParameters(operation).toList())
                    .addCode(contract.operationBody(operation));
            if (operation.typeParameters().isPresent()) {
                for (var typeParameter : operation.typeParameters().get()) {
                    spec.addTypeVariable(getTypeNames().typeVariableName(typeParameter));
                }
            }
            return spec.build();
        } catch (SpecError e) {
            throw OperationGenerationError.of(operation.name(), e);
        }
    }

    Stream<ParameterSpec> generateRequestFactoryOperationParameters(Operation operation) {
        var includeMountPath = contract.operationNeedsExplicitMountPath(operation);
        var parameters = operation.getParameters().stream()
                .filter(parameter -> {
                    if (parameter.includeIn().isPresent()) {
                        return parameter.includeIn().get().contains(contract.getName());
                    } else {
                        return true;
                    }
                })
                .map(parameter -> generateRequestFactoryOperationParameter(operation, parameter));
        if (includeMountPath) {
            parameters = Stream.concat(Stream.of(ParameterSpec.builder(String.class, "mountPath").build()), parameters);
        }
        return parameters;
    }

    ParameterSpec generateRequestFactoryOperationParameter(Operation operation, Operation.Parameter parameter) {

        TypeName paramTypeName;
        if (parameter.object().isPresent()) {

            var typeName = typeNameFor(operation.name(), parameter.name());
            addGeneratedType(typeName,
                    (name) -> generatePOJO(name, PartialPOJO.of(parameter.object().get()), operation.name()));
            paramTypeName = typeName;

        } else if (parameter.type().isPresent()) {

            paramTypeName = typeName(parameter.type().get());

        } else {
            throw new RuntimeException("Parameter '" + parameter.name() + "' must have a type or an object");
        }

        if (parameter.isRequired() && paramTypeName.isBoxedPrimitive()) {
            paramTypeName = paramTypeName.unbox();
        } else if (!parameter.isRequired() && !paramTypeName.isPrimitive()) {
            paramTypeName = paramTypeName.box();
        }

        var paramSpec = ParameterSpec.builder(paramTypeName, parameter.name());

        if (!parameter.isRequired()) {
            paramSpec.addAnnotation(className(Nullable.class));
        }

        return paramSpec.build();
    }

}

package io.quarkus.vault.generator;

import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.model.Operation;

public class APIContract extends BaseAPIGenerator implements APIGenerator.Contract {

    private static final ClassName UNI_TYPE_NAME = ClassName.get("io.smallrye.mutiny", "Uni");

    private final ClassName apiName;
    private final ClassName mountableApiName;
    private final ClassName requestExecutorTypeName;
    private final ClassName requestFactoryTypeName;
    private final ClassName responseTypeName;

    public APIContract(API api) {
        super(api);
        apiName = className(api.getCommonAPIPackageName(), "VaultAPI");
        mountableApiName = className(api.getCommonAPIPackageName(), "VaultMountableAPI");
        requestExecutorTypeName = className(api.getCommonPackageName(), "VaultRequestExecutor");
        requestFactoryTypeName = typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX);
        responseTypeName = className(api.getCommonPackageName(), "VaultResponse");
    }

    @Override
    public ClassName typeName() {
        return typeNameFor();
    }

    @Override
    public TypeSpec.Builder start() {
        var typeSpec = TypeSpec.classBuilder(typeName())
                .addModifiers(Modifier.PUBLIC)
                .addField(
                        FieldSpec.builder(requestFactoryTypeName, "FACTORY")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                                .initializer("$T.INSTANCE", requestFactoryTypeName)
                                .build())
                .addField(
                        FieldSpec.builder(typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX), "factory")
                                .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
                                .build());

        if (api.isMountable()) {
            typeSpec.superclass(mountableApiName);
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(String.class, "mountPath")
                            .addParameter(typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX), "factory")
                            .addStatement("super(executor, mountPath)")
                            .addStatement("this.factory = factory")
                            .build());
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(String.class, "mountPath")
                            .addStatement("this(executor, mountPath, FACTORY)")
                            .build());
        } else {
            typeSpec.superclass(apiName);
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX), "factory")
                            .addStatement("super(executor)")
                            .addStatement("this.factory = factory")
                            .build());
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addStatement("this(executor, FACTORY)")
                            .build());
        }

        return typeSpec;
    }

    @Override
    public TypeName operationReturnTypeName(TypeName type) {
        return typeName(UNI_TYPE_NAME, type);
    }

    @Override
    public boolean operationNeedsExplicitMountPath(Operation operation) {
        return false;
    }

    @Override
    public CodeBlock operationBody(Operation operation) {

        var body = CodeBlock.builder();

        Stream<String> parameterNames;
        if (api.isMountable()) {
            parameterNames = Stream.concat(Stream.of("mountPath"),
                    operation.getParameters().stream().map(Operation.Parameter::name));
        } else {
            parameterNames = operation.getParameters().stream().map(Operation.Parameter::name);
        }

        body.add("$[return executor.execute(factory.$L($L))\n", operation.name(),
                parameterNames.map(CodeBlock::of).collect(CodeBlock.joining(", ")));

        body.add(".map($T::getResult);$]", responseTypeName);

        return body.build();
    }

    @Override
    public TypeName operationResultTypeName(Operation operation) {
        if (operation.getStatus().isEmpty()) {
            return TypeName.INT.box();
        }
        if (operation.result().isEmpty()) {
            return TypeName.VOID.box();
        }

        var result = operation.result().get();
        if (result instanceof Operation.RawResult rawResult) {
            return typeName(rawResult.type());
        } else if (result instanceof Operation.JSONResult jsonResult && jsonResult.type().isPresent()) {
            return typeName(jsonResult.type().get());
        } else {
            return typeNameFor(operation.name(), "Result");
        }
    }
}

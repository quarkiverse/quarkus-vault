package io.quarkus.vault.generator;

import static io.quarkus.vault.generator.utils.Strings.capitalize;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import io.quarkus.vault.generator.errors.OperationGenerationError;
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

    public String getName() {
        return "API";
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
                                .build());

        var factoryTypeName = typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX);

        if (api.isMountable()) {
            typeSpec.superclass(typeName(mountableApiName, factoryTypeName));
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(String.class, "mountPath")
                            .addParameter(factoryTypeName, "factory")
                            .addStatement("super(executor, factory, mountPath)")
                            .build());
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(String.class, "mountPath")
                            .addStatement("this(executor, mountPath, FACTORY)")
                            .build());
        } else {
            typeSpec.superclass(typeName(apiName, factoryTypeName));
            typeSpec.addMethod(
                    MethodSpec.constructorBuilder()
                            .addModifiers(Modifier.PUBLIC)
                            .addParameter(requestExecutorTypeName, "executor")
                            .addParameter(typeNameFor(APIRequestFactoryContract.TYPE_NAME_SUFFIX), "factory")
                            .addStatement("super(executor, factory)")
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

        Predicate<Operation.Parameter> includedInFactory = parameter -> {
            if (parameter.includeIn().isPresent()) {
                return parameter.includeIn().get().contains("Factory");
            } else {
                return true;
            }
        };

        Stream<String> parameterNames;
        if (api.isMountable()) {
            parameterNames = Stream.concat(Stream.of("mountPath"),
                    operation.getParameters().stream()
                            .filter(includedInFactory)
                            .map(Operation.Parameter::name));
        } else {
            parameterNames = operation.getParameters().stream()
                    .filter(includedInFactory)
                    .map(Operation.Parameter::name);
        }

        body.add("$[return executor.execute(factory.$L($L))\n", operation.name(),
                parameterNames.map(CodeBlock::of).collect(CodeBlock.joining(", ")));

        body.add(".map($T::getResult)", responseTypeName);

        if (operation.result().isPresent() && operation.result().get() instanceof Operation.LeasedResult leasedResult) {
            if (leasedResult.unwrapUsing().isPresent()) {
                body.add(".map(r -> ");

                var unwrapUsingArguments = leasedResult.unwrapUsingArguments().orElse(List.of()).stream()
                        .map(argument -> {
                            if (argument.startsWith("<type>")) {
                                return typeName(argument.substring("<type>".length()));
                            } else {
                                return argument;
                            }
                        });
                body.add(leasedResult.unwrapUsing().get(), unwrapUsingArguments.toArray());

                body.add(")");
            } else if (leasedResult.unwrapData().orElse(false)) {
                body.add(".map($T::getData)", typeName("$$.api.common.VaultLeasedResult"));
            } else if (leasedResult.unwrapAuth().orElse(false)) {
                body.add(".map($T::getAuth)", typeName("$$.api.common.VaultLeasedResult"));
            }
        }

        operation.recoverNotFound().ifPresent(recoverNotFound -> {
            var recoverArguments = recoverNotFound.arguments().orElse(List.of()).stream()
                    .map(argument -> {
                        if (argument.startsWith("<type>")) {
                            return typeName(argument.substring("<type>".length()));
                        } else {
                            return argument;
                        }
                    });

            body.add("\n.onFailure($T.INSTANCE)\n", typeName("$$.api.common.VaultNotFoundPredicate"));
            body.add(".recoverWithItem(() -> " + recoverNotFound.using() + ")", recoverArguments.toArray());
        });

        body.add(";$]");

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
        } else if (result instanceof Operation.JSONResult jsonResult) {
            if (jsonResult.type().isPresent()) {
                return typeName(jsonResult.type().get());
            } else {
                return typeNameFor(operation.name(), "Result");
            }
        } else if (result instanceof Operation.LeasedResult leasedResult) {
            if (leasedResult.unwrappedType().isPresent()) {
                return typeName(leasedResult.unwrappedType().get());
            } else if (leasedResult.unwrapData().orElse(false)) {
                if (leasedResult.dataType().isPresent()) {
                    return typeName(leasedResult.dataType().get());
                } else {
                    return typeNameFor(capitalize(operation.name()), "ResultData");
                }
            } else if (leasedResult.unwrapAuth().orElse(false)) {
                return typeNameFor(capitalize(operation.name()), "AuthResult");
            } else {
                return typeNameFor(operation.name(), "Result");
            }
        } else {
            throw new OperationGenerationError(operation.name(), "Unsupported result type: " + result.getClass().getName(),
                    null);
        }
    }
}

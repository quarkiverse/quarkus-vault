package io.quarkus.vault.generator;

import static io.quarkus.vault.generator.utils.Strings.capitalize;

import java.util.*;
import java.util.stream.Collectors;

import javax.lang.model.element.Modifier;

import com.squareup.javapoet.*;

import io.quarkus.vault.generator.errors.OneOfFieldsMissingError;
import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.model.AnyPOJO;
import io.quarkus.vault.generator.model.Operation;
import io.quarkus.vault.generator.model.PartialPOJO;

public class APIRequestFactoryContract extends BaseAPIGenerator implements APIGenerator.Contract {

    static final String TYPE_NAME_SUFFIX = "RequestFactory";

    private final ClassName requestFactoryTypeName;
    private final ClassName requestTypeName;
    private final ClassName jsonResultTypeName;
    private final ClassName leasedResultTypeName;
    private final ClassName authResultTypeName;
    private final ClassName statusResultTypeName;
    private final ClassName leasedExtractorTypeName;
    private final ClassName jsonExtractorTypeName;
    private final ClassName statusExtractorTypeName;
    private final ClassName binaryExtractorTypeName;

    public APIRequestFactoryContract(API api) {
        super(api);
        requestTypeName = className(api.getCommonPackageName(), "VaultRequest");
        requestFactoryTypeName = className(api.getCommonAPIPackageName(), "VaultRequestFactory");
        jsonResultTypeName = className(api.getCommonPackageName(), "VaultJSONResult");
        leasedResultTypeName = className(api.getCommonAPIPackageName(), "VaultLeasedResult");
        authResultTypeName = className(api.getCommonAPIPackageName(), "VaultAuthResult");
        statusResultTypeName = className(api.getCommonPackageName(), "VaultStatusResult");
        leasedExtractorTypeName = className(api.getCommonPackageName(), "VaultLeasedResultExtractor");
        jsonExtractorTypeName = className(api.getCommonPackageName(), "VaultJSONResultExtractor");
        statusExtractorTypeName = className(api.getCommonPackageName(), "VaultStatusResultExtractor");
        binaryExtractorTypeName = className(api.getCommonPackageName(), "VaultBinaryResultExtractor");
    }

    @Override
    public ClassName typeName() {
        return typeNameFor(TYPE_NAME_SUFFIX);
    }

    @Override
    public TypeSpec.Builder start() {
        var typeName = typeName();
        return TypeSpec.classBuilder(typeName)
                .addModifiers(Modifier.PUBLIC)
                .superclass(requestFactoryTypeName)
                .addField(
                        FieldSpec.builder(typeName, "INSTANCE")
                                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                                .initializer("new $T()", typeName)
                                .build())
                .addMethod(
                        MethodSpec.constructorBuilder()
                                .addModifiers(Modifier.PUBLIC)
                                .addStatement("super($S)", api.getTraceNamePrefix())
                                .build());
    }

    @Override
    public TypeName operationReturnTypeName(TypeName type) {
        return typeName(requestTypeName, type);
    }

    @Override
    public boolean operationNeedsExplicitMountPath(Operation operation) {
        return api.isMountable();
    }

    @Override
    public CodeBlock operationBody(Operation operation) {
        var method = operation.getMethod();

        var body = CodeBlock.builder();

        body.add("$[return $T." + method.getBuilderMethodName(), requestTypeName);

        if (api.isMountable()) {
            body.add("(getTraceOpName($S), mountPath)\n", operation.getTraceTitle());
        } else {
            body.add("(getTraceOpName($S), null)\n", operation.getTraceTitle());
        }

        if (operation.pathChoice().isPresent()) {
            addPathChoice(body, operation, operation.pathChoice().get());
        } else if (operation.path().isPresent()) {
            addPath(body, operation.path().get());
        } else {
            throw OneOfFieldsMissingError.of("No path specified", "path", "pathChoice");
        }

        if (!operation.isAuthenticated()) {
            body.add(".noToken()\n");
        }

        if (operation.tokenFrom().isPresent()) {
            body.add(".token($L)\n", operation.tokenFrom().get());
        }

        if (operation.wrapTTLFrom().isPresent()) {
            body.add(".wrapTTL($L)\n", operation.wrapTTLFrom().get());
        }

        if (operation.queryFrom().isPresent()) {
            for (var queryParamName : operation.queryFrom().get()) {
                var queryParam = operation.getRequiredParameter(queryParamName);
                if (queryParam.isIncludeNulls()) {
                    body.add(".queryParam($S, $L)\n", queryParam.name(), queryParam.name());
                } else {
                    body.add(".queryParam($L != null, $S, $L)\n", queryParam.name(), queryParam.name(), queryParam.name());
                }
            }
        }

        if (operation.headers().isPresent()) {
            for (var header : operation.headers().get().entrySet()) {
                String headerName = header.getKey();
                String headerValue = header.getValue();
                if (headerValue.startsWith(":")) {
                    var headerParam = operation.getRequiredParameter(headerValue.substring(1));
                    if (headerParam.isIncludeNulls()) {
                        body.add(".header($S, $L)\n", headerName, headerParam.name());
                    } else {
                        body.add(".header($L != null, $S, $L)\n", headerParam.name(), headerName, headerParam.name());
                    }
                } else {
                    body.add(".header($S, $S)\n", headerName, headerValue);
                }
            }
        }

        if (operation.bodyFrom().isPresent()) {

            var bodyPropertyNames = operation.bodyFrom().get();

            TypeName bodyTypeName;
            if (operation.bodyType().isPresent()) {

                bodyTypeName = typeName(operation.bodyType().get());

            } else {

                var bodyProperties = bodyPropertyNames.stream()
                        .map(operation::getRequiredParameter)
                        .map(Operation.Parameter::asProperty)
                        .toList();

                var objTypeName = typeNameFor(operation.name(), "Params");
                addGeneratedType(objTypeName, (name) -> generatePOJO(name, PartialPOJO.of(bodyProperties)));
                bodyTypeName = objTypeName;
            }

            body.add(".body(new $T()$>", bodyTypeName);

            for (var bodyPropertyName : bodyPropertyNames) {
                body.add("\n.set$L($L)", capitalize(bodyPropertyName), bodyPropertyName);
            }

            body.add("$<)\n");
        } else if (operation.getMethod().allowsBody && operation.getBodyParameter().isPresent()) {
            var bodyParameter = operation.getBodyParameter().get();
            body.add(".body($L)\n", bodyParameter.name());
        }

        addExpectedStatus(operation, body);
        addBuild(operation, body);

        body.add(";$]\n");

        return body.build();
    }

    private void addPathChoice(CodeBlock.Builder body, Operation operation, Operation.PathChoice pathChoice) {
        var param = operation.getRequiredParameter(pathChoice.param());
        var paramType = typeName(param.getImpliedType());

        body.add(".pathChoice($L,$>\n", param.name());
        if (paramType.unbox().equals(TypeName.BOOLEAN)) {
            addPathChoiceSegments(body, pathSegments(pathChoice.getRequiredChoice(true).path()));
            body.add(",\n");
            addPathChoiceSegments(body, pathSegments(pathChoice.getRequiredChoice(false).path()));
        } else if (paramType.equals(ClassName.get(String.class))) {
            body.add("Map.of($>\n");
            var choices = new ArrayList<CodeBlock>();
            for (var choice : pathChoice.choices()) {
                choices.add(addPathChoice(choice.value().toString(), pathSegments(choice.path())));
            }
            body.add(choices.stream().collect(CodeBlock.joining(",\n")));
            body.add("$<\n)");
        }
        body.add(")$<\n");
    }

    private CodeBlock addPathChoice(String choiceValue, List<Map.Entry<String, String>> pathSegments) {
        var body = CodeBlock.builder();
        body.add("$S, ", choiceValue);
        addPathChoiceSegments(body, pathSegments);
        body.add(",\n");
        return body.build();
    }

    private void addPathChoiceSegments(CodeBlock.Builder body, List<Map.Entry<String, String>> pathSegments) {
        body.add("new $T[] {", String.class);
        var formats = pathSegments.stream().map(Map.Entry::getKey).collect(Collectors.joining(", "));
        var args = pathSegments.stream().map(Map.Entry::getValue).toArray();
        body.add(formats, args);
        body.add("}");
    }

    private void addPath(CodeBlock.Builder body, String path) {
        var pathParts = pathSegments(path);
        var pathFormat = String.join(", ", pathParts.stream().map(Map.Entry::getKey).toList());
        body.add(".path(" + pathFormat + ")\n", pathParts.stream().map(Map.Entry::getValue).toArray());
    }

    private List<Map.Entry<String, String>> pathSegments(String path) {
        var pathParts = path.split("/");
        var pathSegments = new ArrayList<Map.Entry<String, String>>();
        if (api.basePath().isPresent()) {
            pathSegments.add(Map.entry("$S", api.basePath().get()));
        }
        for (var pathPart : pathParts) {
            if (pathPart.startsWith(":")) {
                pathSegments.add(Map.entry("$L", pathPart.substring(1)));
            } else {
                pathSegments.add(Map.entry("$S", pathPart));
            }
        }
        return pathSegments;
    }

    private void addExpectedStatus(Operation operation, CodeBlock.Builder body) {
        if (operation.getStatus().isEmpty()) {
            body.add(".expectAnyStatus()");
        } else {
            switch (operation.getStatus().get()) {
                case OK:
                    body.add(".expectOkStatus()");
                    break;
                case NO_CONTENT:
                    body.add(".expectNoContentStatus()");
                    break;
                case ACCEPTED:
                    body.add(".expectAcceptedStatus()");
                    break;
                case OK_OR_ACCEPTED:
                    body.add(".expectOkOrAcceptedStatus()");
                    break;
            }
        }
        body.add("\n");
    }

    private void addBuild(Operation operation, CodeBlock.Builder body) {
        if (operation.getStatus().isEmpty()) {
            body.add(".build($T.INSTANCE)", statusExtractorTypeName);
            return;
        } else if (operation.result().isEmpty()) {
            body.add(".build()");
            return;
        }

        var resultTypeName = operationResultTypeName(operation);
        switch (operation.result().get().kind()) {
            case Leased -> body.add(".build($T.of($T.class))", leasedExtractorTypeName, resultTypeName);
            case JSON -> body.add(".build($T.of($T.class))", jsonExtractorTypeName, resultTypeName);
            case Raw -> body.add(".build($T.of($T.class))", binaryExtractorTypeName, resultTypeName);
        }
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
        return switch (result.kind()) {
            case Leased -> generateLeasedResult(operation, (Operation.LeasedResult) result);
            case JSON -> generateJSONResult(operation, (Operation.JSONResult) result);
            case Raw -> typeName(((Operation.RawResult) result).type());
        };
    }

    private TypeName generateJSONResult(Operation operation, Operation.JSONResult result) {
        if (result.type().isPresent()) {
            return typeName(result.type().get());
        } else if (result.object().isPresent()) {
            var resultTypeName = typeNameFor(operation.name(), "Result");
            addGeneratedType(resultTypeName,
                    (name) -> generatePOJO(name, PartialPOJO.of(result.object().get()),
                            s -> s.addSuperinterface(jsonResultTypeName)));
            return resultTypeName;
        } else {
            throw OneOfFieldsMissingError.of("No type or definition specified", "type", "object");
        }
    }

    private TypeName generateLeasedResult(Operation operation, Operation.LeasedResult result) {

        TypeName dataTypeName;
        if (result.data().isPresent()) {
            ClassName typeName = typeNameFor(operation.name(), "ResultData");
            addGeneratedType(typeName, (name) -> generatePOJO(name, PartialPOJO.of(result.data().get())));
            dataTypeName = typeName;
        } else if (result.dataType().isPresent()) {
            dataTypeName = typeName(result.dataType().get());
        } else {
            dataTypeName = TypeName.OBJECT;
        }

        TypeName authTypeName;
        if (result.auth().isEmpty() && result.authType().isEmpty()) {
            authTypeName = typeName(authResultTypeName, TypeName.OBJECT);
        } else {
            TypeName authDataTypeName;
            if (result.auth().isPresent()) {
                var typeName = typeNameFor(operation.name(), "AuthData");
                addGeneratedType(typeName, (name) -> generatePOJO(name, PartialPOJO.of(result.auth().get())));
                authDataTypeName = typeName;
            } else {
                authDataTypeName = typeName(result.authType().get());
            }

            var typeName = typeNameFor(operation.name(), "AuthResult");
            var superclass = typeName(authResultTypeName, authDataTypeName);

            addGeneratedType(typeName, (name) -> generatePOJO(name, AnyPOJO.empty(), s -> s.superclass(superclass)));

            authTypeName = typeName;
        }

        var resultTypeName = typeNameFor(operation.name(), "Result");
        var baseTypeName = typeName(leasedResultTypeName, dataTypeName, authTypeName);

        if (result.custom().isPresent()) {

            var resultPOJO = result.custom().get();

            addGeneratedType(resultTypeName, (name) -> generatePOJO(name, resultPOJO, s -> s.superclass(baseTypeName)));
        } else {

            addGeneratedType(resultTypeName, (name) -> generatePOJO(name, AnyPOJO.empty(), s -> s.superclass(baseTypeName)));
        }

        return resultTypeName;
    }
}

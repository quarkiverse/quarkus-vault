package io.quarkus.vault.generator;

import static io.quarkus.vault.generator.utils.Strings.capitalize;
import static io.quarkus.vault.generator.utils.Strings.decapitalize;
import static javax.lang.model.element.Modifier.*;

import java.util.List;
import java.util.Map;

import com.squareup.javapoet.*;

import io.quarkus.vault.generator.model.API;
import io.quarkus.vault.generator.utils.TypeNames;

public class AccessorGenerator {

    private static final String CLASS_NAME = "Vault%sAccessor";
    private static final String REQ_EXEC_CLASS_NAME = "VaultRequestExecutor";
    private static final Map<String, String> DEFAULT_MOUNT_PATHS = Map.of(
            "kv1", "secret",
            "kv2", "secret");

    private final String categoryName;
    private final List<API> apis;

    public AccessorGenerator(String categoryName, List<API> apis) {
        this.categoryName = categoryName;
        this.apis = apis;
    }

    public JavaFile generate() {

        var accAPI = API.EMPTY;

        var className = ClassName.get(accAPI.getAPIPackageName(), CLASS_NAME.formatted(capitalize(categoryName)));
        var reqExecTypeName = ClassName.get(accAPI.getCommonPackageName(), REQ_EXEC_CLASS_NAME);

        var typeSpec = TypeSpec.classBuilder(className)
                .addModifiers(PUBLIC)
                .addMethod(MethodSpec.constructorBuilder()
                        .addModifiers(PUBLIC)
                        .addParameter(ParameterSpec.builder(reqExecTypeName, "executor").build())
                        .addStatement("this.executor = executor")
                        .build())
                .addField(reqExecTypeName, "executor", PRIVATE, FINAL)
                .addMethod(MethodSpec.methodBuilder("getExecutor")
                        .addModifiers(PUBLIC)
                        .returns(reqExecTypeName)
                        .addStatement("return executor")
                        .build());

        for (var api : apis) {
            var typeNames = new TypeNames(api);
            var apiClassName = typeNames.typeNameFor();
            var apiName = api.getName().toLowerCase();

            // Skip empty APIs
            if (apiName.isEmpty()) {
                continue;
            }

            if (api.isMountable()) {

                var defaultMountPath = DEFAULT_MOUNT_PATHS
                        .getOrDefault(api.getName().toLowerCase(), api.getName().toLowerCase());

                var defaultMountPathConst = "DEFAULT_%S_MOUNT_PATH".formatted(apiName.toUpperCase());
                typeSpec.addField(
                        FieldSpec.builder(String.class, defaultMountPathConst)
                                .addModifiers(PUBLIC, STATIC, FINAL)
                                .initializer("$S", defaultMountPath)
                                .build());

                typeSpec.addMethod(MethodSpec.methodBuilder(decapitalizeCategory(api.getName()))
                        .addModifiers(PUBLIC)
                        .returns(apiClassName)
                        .addStatement("return new $T(executor, $L)", apiClassName, defaultMountPathConst)
                        .build());

                typeSpec.addMethod(MethodSpec.methodBuilder(decapitalizeCategory(api.getName()))
                        .addModifiers(PUBLIC)
                        .returns(apiClassName)
                        .addParameter(ParameterSpec.builder(String.class, "mountPath").build())
                        .addStatement("return new $T(executor, mountPath)", apiClassName)
                        .build());
            } else {

                typeSpec.addMethod(MethodSpec.methodBuilder(decapitalizeCategory(api.getName()))
                        .addModifiers(PUBLIC)
                        .returns(apiClassName)
                        .addStatement("return new $T(executor)", apiClassName)
                        .build());
            }
        }

        return JavaFile.builder(className.packageName(), typeSpec.build()).build();
    }

    private static String decapitalizeCategory(String str) {
        if (str.equals(str.toUpperCase())) {
            return str.toLowerCase();
        } else {
            return decapitalize(str);
        }
    }

}

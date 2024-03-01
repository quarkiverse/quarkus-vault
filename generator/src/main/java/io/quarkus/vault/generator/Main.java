package io.quarkus.vault.generator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.vault.generator.errors.SpecError;
import io.quarkus.vault.generator.model.API;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0 || args.length > 3) {
            throw new IllegalArgumentException("Usage: java -jar generator.jar <specs directory> <target directory> [-clean]");
        }

        var specsPath = Path.of(args[0]);
        var targetPath = Path.of(args[1]);

        var clean = args.length == 3 && args[2].equals("-clean");

        if (!Files.exists(specsPath)) {
            throw new IllegalArgumentException("Specs directory does not exist: " + specsPath);
        }

        if (Files.exists(targetPath)) {
            if (clean) {
                System.out.println("Deleting existing Java files from " + targetPath);
                deleteJavaFiles(targetPath);
            }
        } else {
            if (!targetPath.toFile().mkdirs()) {
                throw new RuntimeException("Could not create target directory: " + targetPath);
            }
        }

        System.out.println("Generating specs to " + targetPath);

        var targetSpecsPath = targetPath.resolve(".specs");

        try {

            var apis = new ArrayList<API>();

            try (var specPaths = Files.walk(specsPath)) {
                specPaths.filter(path -> path.toString().endsWith(".yaml"))
                        .forEach(path -> {
                            var relativePath = specsPath.relativize(path);

                            try {
                                var targetSpecPath = targetSpecsPath.resolve(relativePath);
                                targetSpecPath.getParent().toFile().mkdirs();

                                System.out.println("Generating spec: " + relativePath);

                                var api = generateAPI(path.toUri().toURL(), targetPath);
                                apis.add(api);

                                Files.copy(specsPath, targetSpecPath, REPLACE_EXISTING);

                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });

                generateAccessors(apis, targetPath);
            }

        } catch (Throwable e) {
            var specError = SpecError.unwrap(e);
            if (specError.isPresent()) {
                throw specError.get().withoutCause();
            } else {
                throw e;
            }
        }
    }

    static void generateAccessors(ArrayList<API> apis, Path dir) throws Exception {
        var groupedByCategory = apis.stream().collect(Collectors.<API, String> groupingBy(API::getCategory));
        for (var entry : groupedByCategory.entrySet()) {

            if (entry.getKey().isEmpty()) {
                continue;
            }

            var accessorsGenerator = new AccessorGenerator(entry.getKey(), entry.getValue());
            var file = accessorsGenerator.generate();
            try {
                file.writeToPath(dir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    static API generateAPI(URL url, Path dir) throws Exception {

        var mapper = new YAMLMapper().findAndRegisterModules();
        var api = mapper.readValue(url, API.class);

        var requestFactoryContract = new APIRequestFactoryContract(api);
        var requestFactoryGenerator = new APIGenerator(api, requestFactoryContract);
        requestFactoryGenerator.generate()
                .forEach(javaFile -> {
                    try {
                        javaFile.writeToPath(dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        var apiContract = new APIContract(api);
        var apiGenerator = new APIGenerator(api, apiContract);
        apiGenerator.generate()
                .forEach(javaFile -> {
                    try {
                        javaFile.writeToPath(dir);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });

        return api;
    }

    static void deleteJavaFiles(Path dir) throws IOException {
        try (var files = Files.walk(dir)) {
            files.forEach(path -> {
                if (Files.isRegularFile(path) && path.toString().endsWith("java")) {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

}

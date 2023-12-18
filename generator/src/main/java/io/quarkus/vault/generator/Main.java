package io.quarkus.vault.generator;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;

import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;

import io.quarkus.vault.generator.errors.SpecError;
import io.quarkus.vault.generator.model.API;

public class Main {

    public static void main(String[] args) throws Exception {

        if (args.length == 0 || args.length > 2) {
            throw new IllegalArgumentException("Usage: java -jar generator.jar <specs directory> <target directory>");
        }

        var specsPath = Path.of(args[0]);
        if (!Files.exists(specsPath)) {
            throw new IllegalArgumentException("Specs directory does not exist: " + specsPath);
        }

        var targetPath = Path.of(args[1]);
        if (Files.exists(targetPath)) {
            deleteJavaFiles(targetPath);
        } else {
            if (!targetPath.toFile().mkdirs()) {
                throw new RuntimeException("Could not create target directory: " + targetPath);
            }
        }

        System.out.println("Generating specs to " + targetPath);

        var targetSpecsPath = targetPath.resolve(".specs");

        try {

            try (var specPaths = Files.walk(specsPath)) {
                specPaths.filter(path -> path.toString().endsWith(".yaml"))
                        .forEach(path -> {
                            var relativePath = specsPath.relativize(path);

                            try {
                                var targetSpecPath = targetSpecsPath.resolve(relativePath);
                                targetSpecPath.getParent().toFile().mkdirs();

                                System.out.println("Copying spec to " + targetSpecPath);
                                Files.copy(specsPath, targetSpecPath, REPLACE_EXISTING);

                                System.out.println("Generating spec: " + relativePath);

                                generate(path.toUri().toURL(), targetPath);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

        } catch (Throwable e) {
            var specError = SpecError.unwrap(e);
            if (specError.isPresent()) {
                specError.get().print("");
            } else {
                throw e;
            }
        }
    }

    static void generate(URL url, Path dir) throws Exception {
        try (var stream = url.openStream()) {

            var mapper = new YAMLMapper().findAndRegisterModules();
            var api = mapper.readValue(stream, API.class);

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
        }
    }

    static void deleteJavaFiles(Path dir) throws IOException {
        try (var files = Files.walk(dir)) {
            files.forEach(path -> {
                if (Files.isRegularFile(path) && path.toString().endsWith("java")) {
                    try {
                        System.out.println("Deleting " + path);
                        Files.delete(path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }

}

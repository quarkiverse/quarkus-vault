package io.quarkus.vault;

import static io.quarkus.devtools.codestarts.quarkus.QuarkusCodestartCatalog.Language.JAVA;
import static io.quarkus.devtools.testing.SnapshotTesting.checkContains;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.devtools.testing.codestarts.QuarkusCodestartTest;

public class VaultCodestartTest {

    @RegisterExtension
    public static QuarkusCodestartTest codestartTest = QuarkusCodestartTest.builder()
            .setupStandaloneExtensionTest("io.quarkiverse.vault:quarkus-vault")
            .languages(JAVA)
            .build();

    @Test
    void testContent() throws Throwable {
        codestartTest.checkGeneratedSource("org.acme.VaultSecretResource");
        codestartTest.checkGeneratedTestSource("org.acme.VaultSecretResourceTest");
        codestartTest.assertThatGeneratedFile(JAVA, "src/main/resources/application.properties")
                .satisfies(checkContains(
                        "quarkus.vault.devservices.init-commands=kv put secret/myapps/vault-codestart greeting=hello-from-vault"))
                .satisfies(checkContains("quarkus.vault.secret-config-kv-path=myapps/vault-codestart"));
        codestartTest.assertThatGeneratedFileMatchSnapshot(JAVA, "README.md");
    }

    @Test
    void buildAllProjects() throws Throwable {
        codestartTest.buildAllProjects();
    }
}

package io.quarkus.vault.client;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "kubernetes", path = "kubernetes")
})
public class VaultKubernetesAuthTest {

    @Test
    public void testLoginProcess(VaultClient client, @Random String role) {
        var kubernetesApi = client.auth().kubernetes();
    }

}

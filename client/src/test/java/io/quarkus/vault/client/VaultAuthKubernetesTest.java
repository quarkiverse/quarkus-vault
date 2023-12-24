package io.quarkus.vault.client;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import io.fabric8.kubernetes.api.model.ServiceAccountBuilder;
import io.fabric8.kubernetes.api.model.authentication.TokenRequestBuilder;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBindingBuilder;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetesConfigureParams;
import io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetesUpdateRoleParams;
import io.quarkus.vault.client.auth.VaultKubernetesAuthOptions;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;
import io.smallrye.mutiny.Uni;

@VaultClientTest(auths = {
        @Mount(type = "kubernetes", path = "kubernetes")
})
@Testcontainers
public class VaultAuthKubernetesTest {

    @Container
    private final K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:latest"))
            .withNetworkAliases("kubernetes")
            .withNetwork(Network.SHARED);

    @Test
    public void testLoginProcess(VaultClient rootClient, @Random String role) {
        var serviceAccount = createServiceAccount(role);
        var policyName = createTestPolicy(role, rootClient);
        configureK8sAuth(role, serviceAccount, policyName, rootClient);

        var k8sClient = rootClient.configure().kubernetes(
                VaultKubernetesAuthOptions.builder()
                        .role(role)
                        .jwtProvider(() -> Uni.createFrom().item(serviceAccount.token()))
                        .build())
                .build();

        k8sClient.secrets().kv2().updateSecret(role, null, Map.of("foo", "bar"))
                .await().indefinitely();

        var secret = k8sClient.secrets().kv2().readSecret(role)
                .await().indefinitely();

        assertThat(secret)
                .isNotNull();
        assertThat(secret.getData())
                .containsEntry("foo", "bar");
    }

    private void configureK8sAuth(String role, ServiceAccountInfo serviceAccount, String policyName, VaultClient client) {
        var k3sInternalConfig = Config.fromKubeconfig(k3s.generateInternalKubeConfigYaml("kubernetes"));

        client.auth().kubernetes().configure(new VaultAuthKubernetesConfigureParams()
                .setKubernetesHost(k3sInternalConfig.getMasterUrl())
                .setKubernetesCaCert(new String(Base64.getDecoder().decode(k3sInternalConfig.getCaCertData()), UTF_8)))
                .await().indefinitely();

        client.auth().kubernetes().updateRole(role, new VaultAuthKubernetesUpdateRoleParams()
                .setBoundServiceAccountNames(List.of(serviceAccount.name()))
                .setBoundServiceAccountNamespaces(List.of(serviceAccount.namespace()))
                .setTokenNoDefaultPolicy(true)
                .setTokenPolicies(List.of(policyName))
                .setTokenTtl("1h")
                .setTokenMaxTtl("2h"))
                .await().indefinitely();
    }

    private String createTestPolicy(String role, VaultClient client) {
        var policyName = role + "-policy";
        client.sys().policy().update(policyName, """
                path "secret/data/%s" {
                  capabilities = ["create", "read", "update", "delete", "list"]
                }
                """.formatted(role))
                .await().indefinitely();
        return policyName;
    }

    private record ServiceAccountInfo(String name, String namespace, String token) {
    }

    private ServiceAccountInfo createServiceAccount(String role) {
        var k3sConfig = Config.fromKubeconfig(k3s.getKubeConfigYaml());

        try (var k8sClient = new KubernetesClientBuilder().withConfig(k3sConfig).build()) {
            var serviceAccount = k8sClient.serviceAccounts()
                    .resource(new ServiceAccountBuilder()
                            .withNewMetadata()
                            .withName(role + "-service-account")
                            .withNamespace("default")
                            .endMetadata()
                            .build())
                    .create();

            k8sClient.rbac().clusterRoleBindings()
                    .resource(new ClusterRoleBindingBuilder()
                            .withNewMetadata()
                            .withName(role + "-auth-delegat-role-binding")
                            .withNamespace("default")
                            .endMetadata()
                            .withNewRoleRef()
                            .withApiGroup("rbac.authorization.k8s.io")
                            .withKind("ClusterRole")
                            .withName("system:auth-delegator")
                            .endRoleRef()
                            .addNewSubject()
                            .withKind(serviceAccount.getKind())
                            .withName(serviceAccount.getMetadata().getName())
                            .withNamespace(serviceAccount.getMetadata().getNamespace())
                            .endSubject()
                            .build())
                    .create();

            var token = k8sClient.serviceAccounts().resource(serviceAccount)
                    .tokenRequest(
                            new TokenRequestBuilder()
                                    .withNewSpec()
                                    .withExpirationSeconds(3600L)
                                    .endSpec()
                                    .build())
                    .getStatus()
                    .getToken();

            return new ServiceAccountInfo(serviceAccount.getMetadata().getName(), serviceAccount.getMetadata().getNamespace(),
                    token);
        }
    }

}

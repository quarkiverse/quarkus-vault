package io.quarkus.vault.client;

import static io.quarkus.vault.client.api.auth.kubernetes.VaultAuthKubernetesAliasNameSource.SERVICEACCOUNT_NAME;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
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
import io.quarkus.vault.client.api.common.VaultTokenType;
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
    private static final K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:latest"))
            .withNetworkAliases("kubernetes")
            .withNetwork(Network.SHARED);

    @Test
    public void testUpdateRole(VaultClient client, @Random String role) {
        var k8sApi = client.auth().kubernetes();

        var policyName = createTestPolicy(role, client);

        k8sApi.updateRole(role, new VaultAuthKubernetesUpdateRoleParams()
                .setBoundServiceAccountNames(List.of("foo"))
                .setBoundServiceAccountNamespaces(List.of("bar"))
                .setAudience("test-audience")
                .setAliasNameSource(SERVICEACCOUNT_NAME)
                .setTokenTtl(Duration.ofHours(1))
                .setTokenMaxTtl(Duration.ofHours(2))
                .setTokenPolicies(List.of(policyName))
                .setTokenBoundCidrs(List.of("127.0.0.1"))
                .setTokenExplicitMaxTtl(Duration.ofHours(3))
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(4)
                .setTokenPeriod(Duration.ofMinutes(30))
                .setTokenType(VaultTokenType.DEFAULT))
                .await().indefinitely();

        var roleInfo = k8sApi.readRole(role)
                .await().indefinitely();

        assertThat(roleInfo)
                .isNotNull();
        assertThat(roleInfo.getBoundServiceAccountNames())
                .contains("foo");
        assertThat(roleInfo.getBoundServiceAccountNamespaces())
                .contains("bar");
        assertThat(roleInfo.getAudience())
                .isEqualTo("test-audience");
        assertThat(roleInfo.getAliasNameSource())
                .isEqualTo(SERVICEACCOUNT_NAME);
        assertThat(roleInfo.getTokenTtl())
                .isEqualTo(Duration.ofHours(1));
        assertThat(roleInfo.getTokenMaxTtl())
                .isEqualTo(Duration.ofHours(2));
        assertThat(roleInfo.getTokenPolicies())
                .containsExactly(policyName);
        assertThat(roleInfo.getTokenBoundCidrs())
                .contains("127.0.0.1");
        assertThat(roleInfo.getTokenExplicitMaxTtl())
                .isEqualTo(Duration.ofHours(3));
        assertThat(roleInfo.isTokenNoDefaultPolicy())
                .isTrue();
        assertThat(roleInfo.getTokenNumUses())
                .isEqualTo(4);
        assertThat(roleInfo.getTokenPeriod())
                .isEqualTo(Duration.ofMinutes(30));
        assertThat(roleInfo.getTokenType())
                .isEqualTo(VaultTokenType.DEFAULT);
    }

    @Test
    public void testDeleteRole(VaultClient client, @Random String role) {

        var k8sApi = client.auth().kubernetes();

        k8sApi.updateRole(role, new VaultAuthKubernetesUpdateRoleParams()
                .setBoundServiceAccountNames(List.of("*"))
                .setBoundServiceAccountNamespaces(List.of("*")))
                .await().indefinitely();

        var roles = k8sApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(role);

        k8sApi.deleteRole(role)
                .await().indefinitely();

        roles = k8sApi.listRoles()
                .await().indefinitely();
        assertThat(roles)
                .doesNotContain(role);
    }

    @Test
    public void testLogin(VaultClient client, @Random String role) {
        var serviceAccount = createServiceAccount(role);
        var policyName = createTestPolicy(role, client);
        configureK8sAuth(role, serviceAccount, policyName, client);

        var loginInfo = client.auth().kubernetes().login(role, serviceAccount.token())
                .await().indefinitely();

        assertThat(loginInfo)
                .isNotNull();
        assertThat(loginInfo.getClientToken())
                .isNotEmpty();
        assertThat(loginInfo.getAccessor())
                .isNotEmpty();
        assertThat(loginInfo.getPolicies())
                .containsExactly(policyName);
        assertThat(loginInfo.getLeaseDuration())
                .isEqualTo(Duration.ofHours(1));
        assertThat(loginInfo.getMetadata().getRole())
                .isEqualTo(role);
        assertThat(loginInfo.getMetadata().getServiceAccountName())
                .isEqualTo(serviceAccount.name());
        assertThat(loginInfo.getMetadata().getServiceAccountNamespace())
                .isEqualTo(serviceAccount.namespace());
        assertThat(loginInfo.getMetadata().getServiceAccountUid())
                .isNotEmpty();
        assertThat(loginInfo.getMetadata().getServiceAccountSecretName())
                .isEmpty();
    }

    @Test
    public void testReadConfig(VaultClient client) {
        var k3sInternalConfig = Config.fromKubeconfig(k3s.generateInternalKubeConfigYaml("kubernetes"));

        var caCert = new String(Base64.getDecoder().decode(k3sInternalConfig.getCaCertData()), UTF_8);

        client.auth().kubernetes().configure(new VaultAuthKubernetesConfigureParams()
                .setKubernetesHost(k3sInternalConfig.getMasterUrl())
                .setKubernetesCaCert(caCert)
                .setIssuer("test-issuer")
                .setPemKeys(List.of(caCert))
                .setDisableLocalCaJwt(true))
                .await().indefinitely();

        var config = client.auth().kubernetes().readConfig()
                .await().indefinitely();

        assertThat(config)
                .isNotNull();
        assertThat(config.getKubernetesHost())
                .isEqualTo(k3sInternalConfig.getMasterUrl());
        assertThat(config.getKubernetesCaCert())
                .isEqualTo(caCert);
        assertThat(config.getIssuer())
                .isEqualTo("test-issuer");
        assertThat(config.getPemKeys())
                .hasSize(1);
        assertThat(config.isDisableLocalCaJwt())
                .isTrue();
        assertThat(config.isDisableIssValidation())
                .isTrue();
    }

    @Test
    public void testClientLogin(VaultClient rootClient, @Random String role) {
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
                .setTokenTtl(Duration.ofHours(1))
                .setTokenMaxTtl(Duration.ofHours(2)))
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

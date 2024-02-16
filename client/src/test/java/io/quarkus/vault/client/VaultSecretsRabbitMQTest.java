package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import io.quarkus.vault.client.api.secrets.rabbitmq.VaultSecretsRabbitMQConfigureConnectionParams;
import io.quarkus.vault.client.api.secrets.rabbitmq.VaultSecretsRabbitMQVHosts;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest(secrets = {
        @VaultClientTest.Mount(path = "rabbitmq", type = "rabbitmq"),
})
@Testcontainers
public class VaultSecretsRabbitMQTest {

    @Container
    public static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management")
            .withNetworkAliases("rmq")
            .withNetwork(Network.SHARED);

    @Test
    public void testConfigureConnection(VaultClient client) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest")
                .setUsernameTemplate("{{.DisplayName}}_{{.RoleName}}"))
                .toCompletableFuture().get();
    }

    @Test
    public void testConfigureLease(VaultClient client) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest"))
                .toCompletableFuture().get();

        rmqApi.configureLease(Duration.ofMinutes(1), Duration.ofMinutes(5))
                .toCompletableFuture().get();

        var config = rmqApi.readLeaseConfig()
                .toCompletableFuture().get();

        assertThat(config.getTtl())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(config.getMaxTtl())
                .isEqualTo(Duration.ofMinutes(5));
    }

    @Test
    public void testUpdateRole(VaultClient client, @Random String roleName) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .toCompletableFuture().get();

        var roleInfo = rmqApi.readRole(roleName)
                .toCompletableFuture().get();

        assertThat(roleInfo.getTags())
                .contains("administrator", "guest");
        assertThat(roleInfo.getVhosts().getVhosts())
                .containsKey("/");
        assertThat(roleInfo.getVhosts().getVhosts().get("/").getRead())
                .isEqualTo(".*");
        assertThat(roleInfo.getVhosts().getVhosts().get("/").getWrite())
                .isEqualTo(".*");
        assertThat(roleInfo.getVhosts().getVhosts().get("/").getConfigure())
                .isEqualTo(".*");
        assertThat(roleInfo.getVhostTopics())
                .isNull();
    }

    @Test
    public void testDeleteRole(VaultClient client, @Random String roleName) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .toCompletableFuture().get();

        var roleInfo = rmqApi.readRole(roleName)
                .toCompletableFuture().get();

        assertThat(roleInfo)
                .isNotNull();

        rmqApi.deleteRole(roleName)
                .toCompletableFuture().get();

        assertThatThrownBy(() -> rmqApi.readRole(roleName)
                .toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
    }

    @Test
    public void testGenerateCredentials(VaultClient client, @Random String roleName) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        configure(client);

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .toCompletableFuture().get();

        rmqApi.configureLease(Duration.ofMinutes(1), Duration.ofMinutes(5))
                .toCompletableFuture().get();

        var credentials = rmqApi.generateCredentials(roleName)
                .toCompletableFuture().get();

        assertThat(credentials)
                .isNotNull();
        assertThat(credentials.getLeaseDuration())
                .isEqualTo(Duration.ofMinutes(1));
        assertThat(credentials.getData())
                .isNotNull();
        assertThat(credentials.getData().getUsername())
                .isNotNull();
        assertThat(credentials.getData().getPassword())
                .isNotNull();
    }

    private void configure(VaultClient client) throws Exception {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest"))
                .toCompletableFuture().get();
    }
}

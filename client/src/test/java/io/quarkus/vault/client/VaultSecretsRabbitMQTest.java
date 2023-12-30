package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

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
    public void testConfigureConnection(VaultClient client) {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest"))
                .await().indefinitely();
    }

    @Test
    public void testConfigureLease(VaultClient client) {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest"))
                .await().indefinitely();

        rmqApi.configureLease(Duration.ofMinutes(1), Duration.ofMinutes(5))
                .await().indefinitely();
    }

    @Test
    public void testUpdateRole(VaultClient client, @Random String roleName) {
        var rmqApi = client.secrets().rabbitMQ();

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .await().indefinitely();

        var roleInfo = rmqApi.readRole(roleName)
                .await().indefinitely();

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
    public void testDeleteRole(VaultClient client, @Random String roleName) {
        var rmqApi = client.secrets().rabbitMQ();

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .await().indefinitely();

        var roleInfo = rmqApi.readRole(roleName)
                .await().indefinitely();

        assertThat(roleInfo)
                .isNotNull();

        rmqApi.deleteRole(roleName)
                .await().indefinitely();

        assertThatThrownBy(() -> rmqApi.readRole(roleName)
                .await().indefinitely())
                .asString().contains("status=404");
    }

    @Test
    public void testGenerateCredentials(VaultClient client, @Random String roleName) {
        var rmqApi = client.secrets().rabbitMQ();

        configure(client);

        var vhosts = new VaultSecretsRabbitMQVHosts()
                .add("/", ".*", ".*", ".*");

        rmqApi.updateRole(roleName, List.of("administrator", "guest"), vhosts, null)
                .await().indefinitely();

        rmqApi.configureLease(Duration.ofMinutes(1), Duration.ofMinutes(5))
                .await().indefinitely();

        var credentials = rmqApi.generateCredentials(roleName)
                .await().indefinitely();

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

    private void configure(VaultClient client) {
        var rmqApi = client.secrets().rabbitMQ();

        rmqApi.configureConnection(new VaultSecretsRabbitMQConfigureConnectionParams()
                .setConnectionUri("http://rmq:15672")
                .setUsername("guest")
                .setPassword("guest"))
                .await().indefinitely();
    }
}

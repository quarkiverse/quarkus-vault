package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.sys.plugins.VaultSysPluginsRegisterParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTestExtension;

@VaultClientTest
public class VaultSysPluginsTest {

    @Test
    public void testList(VaultClient client) {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.list()
                .await().indefinitely();

        assertThat(plugins.getAuth())
                .contains("approle");
        assertThat(plugins.getSecret())
                .contains("kv");
        assertThat(plugins.getDatabase())
                .contains("mysql-database-plugin");
        assertThat(plugins.getDetailed())
                .isNotNull();

        var details = plugins.getDetailed().stream().filter(d -> d.getName().equals("kv")).findFirst().orElseThrow();

        assertThat(details)
                .isNotNull();
        assertThat(details.getName())
                .isEqualTo("kv");
        assertThat(details.getType())
                .isEqualTo("secret");
        assertThat(details.isBuiltin())
                .isTrue();
        assertThat(details.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(details.getVersion())
                .startsWith("v")
                .endsWith("builtin");
    }

    @Test
    public void testListAuths(VaultClient client) {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listAuth()
                .await().indefinitely();

        assertThat(plugins)
                .contains("approle");
    }

    @Test
    public void testListDatabases(VaultClient client) {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listDatabase()
                .await().indefinitely();

        assertThat(plugins)
                .contains("mysql-database-plugin");
    }

    @Test
    public void testListSecrets(VaultClient client) {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listSecret()
                .await().indefinitely();

        assertThat(plugins)
                .contains("kv");
    }

    @Test
    public void testRegister(VaultClient client, @Random String pluginName) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setEnv(List.of("ENV1=VALUE1", "ENV2=VALUE2"))
                .setSha256(pluginSha256))
                .await().indefinitely();

        var pluginInfo = pluginsApi.read("secret", pluginName)
                .await().indefinitely();

        assertThat(pluginInfo)
                .isNotNull();
        assertThat(pluginInfo.getName())
                .isEqualTo(pluginName);
        assertThat(pluginInfo.getCommand())
                .isEqualTo("test-plugin");
        assertThat(pluginInfo.getSha256())
                .isEqualTo(pluginSha256);
        assertThat(pluginInfo.getVersion())
                .isEmpty();
        assertThat(pluginInfo.getArgs())
                .contains("--arg1", "--arg2");
    }

    @Test
    public void testRegisterVersion(VaultClient client, @Random String pluginName) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setSha256(pluginSha256)
                .setVersion("v1.0.0"))
                .await().indefinitely();

        var pluginInfo = pluginsApi.read("secret", pluginName, "v1.0.0")
                .await().indefinitely();

        assertThat(pluginInfo)
                .isNotNull();
        assertThat(pluginInfo.getName())
                .isEqualTo(pluginName);
        assertThat(pluginInfo.getCommand())
                .isEqualTo("test-plugin");
        assertThat(pluginInfo.getSha256())
                .isEqualTo(pluginSha256);
        assertThat(pluginInfo.getVersion())
                .isEqualTo("v1.0.0");
        assertThat(pluginInfo.getArgs())
                .contains("--arg1", "--arg2");
    }

    @Test
    public void testRemove(VaultClient client, @Random String pluginName) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setSha256(pluginSha256))
                .await().indefinitely();

        var pluginInfo = pluginsApi.read("secret", pluginName)
                .await().indefinitely();
        assertThat(pluginInfo)
                .isNotNull();

        pluginsApi.remove("secret", pluginName)
                .await().indefinitely();

        assertThatCode(() -> pluginsApi.read("secret", pluginName)
                .await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

    @Test
    public void testRemoveVersion(VaultClient client, @Random String pluginName) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setSha256(pluginSha256)
                .setVersion("v1.0.0"))
                .await().indefinitely();

        var pluginInfo = pluginsApi.read("secret", pluginName, "v1.0.0")
                .await().indefinitely();
        assertThat(pluginInfo)
                .isNotNull();

        pluginsApi.remove("secret", pluginName, "v1.0.0")
                .await().indefinitely();

        assertThatCode(() -> pluginsApi.read("secret", pluginName, "v1.0.0")
                .await().indefinitely())
                .isInstanceOf(VaultClientException.class)
                .asString().contains("status=404");
    }

    @Test
    public void testReloadPlugin(VaultClient client, @Random String pluginName) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setSha256(pluginSha256)
                .setVersion("v1.0.0"))
                .await().indefinitely();

        pluginsApi.reloadPlugin(pluginName, "global")
                .await().indefinitely();
    }

    @Test
    public void testReloadMounts(VaultClient client, @Random String pluginName, @Random String mount) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginSha256 = VaultClientTestExtension.getPluginSha256();

        pluginsApi.register("secret", pluginName, new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setSha256(pluginSha256)
                .setVersion("v1.0.0"))
                .await().indefinitely();

        client.sys().mounts().enable(mount, pluginName, null, null)
                .await().indefinitely();

        pluginsApi.reloadPlugin(pluginName, "global")
                .await().indefinitely();
    }
}

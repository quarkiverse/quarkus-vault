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
    public void testList(VaultClient client) throws Exception {
        var pluginsApi = client.sys().plugins();

        pluginsApi.register("secret", "test-plugin", new VaultSysPluginsRegisterParams()
                .setCommand("test-plugin")
                .setArgs(List.of("--arg1", "--arg2"))
                .setEnv(List.of("ENV1=VALUE1", "ENV2=VALUE2"))
                .setSha256(VaultClientTestExtension.getPluginSha256())
                .setVersion("v1.0.0"))
                .await().indefinitely();

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

        var kvDetails = plugins.getDetailed().stream().filter(d -> d.getName().equals("kv")).findFirst().orElseThrow();

        assertThat(kvDetails)
                .isNotNull();
        assertThat(kvDetails.getName())
                .isEqualTo("kv");
        assertThat(kvDetails.getType())
                .isEqualTo("secret");
        assertThat(kvDetails.isBuiltin())
                .isTrue();
        assertThat(kvDetails.getDeprecationStatus())
                .isEqualTo("supported");
        assertThat(kvDetails.getVersion())
                .startsWith("v")
                .endsWith("builtin");
        assertThat(kvDetails.getSha256())
                .isNull();

        var testDetails = plugins.getDetailed().stream().filter(d -> d.getName().equals("test-plugin")).findFirst()
                .orElseThrow();

        assertThat(testDetails)
                .isNotNull();
        assertThat(testDetails.getName())
                .isEqualTo("test-plugin");
        assertThat(testDetails.getType())
                .isEqualTo("secret");
        assertThat(testDetails.isBuiltin())
                .isFalse();
        assertThat(testDetails.getDeprecationStatus())
                .isNull();
        assertThat(testDetails.getVersion())
                .isEmpty();
        assertThat(testDetails.getSha256())
                .isEqualTo(VaultClientTestExtension.getPluginSha256());
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

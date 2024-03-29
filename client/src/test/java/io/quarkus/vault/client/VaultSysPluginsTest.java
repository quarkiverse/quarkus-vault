package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.List;
import java.util.concurrent.ExecutionException;

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
                .toCompletableFuture().get();

        var plugins = pluginsApi.list()
                .toCompletableFuture().get();

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
    public void testListAuths(VaultClient client) throws Exception {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listAuth()
                .toCompletableFuture().get();

        assertThat(plugins)
                .contains("approle");
    }

    @Test
    public void testListDatabases(VaultClient client) throws Exception {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listDatabase()
                .toCompletableFuture().get();

        assertThat(plugins)
                .contains("mysql-database-plugin");
    }

    @Test
    public void testListSecrets(VaultClient client) throws Exception {
        var pluginsApi = client.sys().plugins();

        var plugins = pluginsApi.listSecret()
                .toCompletableFuture().get();

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
                .toCompletableFuture().get();

        var pluginInfo = pluginsApi.read("secret", pluginName)
                .toCompletableFuture().get();

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
        assertThat(pluginInfo.getDeprecationStatus())
                .isNull();
    }

    @Test
    public void testReadBuiltin(VaultClient client) throws Exception {
        var pluginsApi = client.sys().plugins();

        var pluginInfo = pluginsApi.read("secret", "kv")
                .toCompletableFuture().get();

        assertThat(pluginInfo)
                .isNotNull();
        assertThat(pluginInfo.getName())
                .isEqualTo("kv");
        assertThat(pluginInfo.getCommand())
                .isEmpty();
        assertThat(pluginInfo.getSha256())
                .isEmpty();
        assertThat(pluginInfo.getVersion())
                .endsWith("builtin");
        assertThat(pluginInfo.getArgs())
                .isNull();
        assertThat(pluginInfo.getDeprecationStatus())
                .isEqualTo("supported");
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
                .toCompletableFuture().get();

        var pluginInfo = pluginsApi.read("secret", pluginName, "v1.0.0")
                .toCompletableFuture().get();

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
                .toCompletableFuture().get();

        var pluginInfo = pluginsApi.read("secret", pluginName)
                .toCompletableFuture().get();
        assertThat(pluginInfo)
                .isNotNull();

        pluginsApi.remove("secret", pluginName)
                .toCompletableFuture().get();

        assertThatCode(() -> pluginsApi.read("secret", pluginName)
                .toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
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
                .toCompletableFuture().get();

        var pluginInfo = pluginsApi.read("secret", pluginName, "v1.0.0")
                .toCompletableFuture().get();
        assertThat(pluginInfo)
                .isNotNull();

        pluginsApi.remove("secret", pluginName, "v1.0.0")
                .toCompletableFuture().get();

        assertThatCode(() -> pluginsApi.read("secret", pluginName, "v1.0.0")
                .toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasFieldOrPropertyWithValue("status", 404);
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
                .toCompletableFuture().get();

        pluginsApi.reloadPlugin(pluginName, "global")
                .toCompletableFuture().get();
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
                .toCompletableFuture().get();

        client.sys().mounts().enable(mount, pluginName, null, null, null)
                .toCompletableFuture().get();

        pluginsApi.reloadPlugin(pluginName, "global")
                .toCompletableFuture().get();
    }
}

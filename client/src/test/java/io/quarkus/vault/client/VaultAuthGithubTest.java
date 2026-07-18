package io.quarkus.vault.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.github.VaultAuthGithubConfigureParams;
import io.quarkus.vault.client.test.GithubMockServer;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;
import io.quarkus.vault.client.test.VaultClientTest.Mount;

@VaultClientTest(auths = {
        @Mount(type = "github", path = "github")
})
public class VaultAuthGithubTest {

    private static final GithubMockServer githubMockServer = new GithubMockServer();

    @BeforeAll
    public static void startGithubMockServer() {
        githubMockServer.start();
    }

    @AfterAll
    public static void stopGithubMockServer() {
        githubMockServer.close();
    }

    @Test
    public void testConfig(VaultClient client) throws Exception {
        var githubApi = client.auth().github();

        githubApi.configure(new VaultAuthGithubConfigureParams()
                .setOrganization(GithubMockServer.ORGANIZATION)
                .setOrganizationId(GithubMockServer.ORGANIZATION_ID)
                .setBaseUrl(githubMockServer.getBaseUrl())
                .setTokenTtl(Duration.ofHours(1))
                .setTokenMaxTtl(Duration.ofHours(2)))
                .toCompletableFuture().get();

        var config = githubApi.readConfig()
                .toCompletableFuture().get();

        assertThat(config.getOrganization())
                .isEqualTo(GithubMockServer.ORGANIZATION);
        assertThat(config.getOrganizationId())
                .isEqualTo(GithubMockServer.ORGANIZATION_ID);
        assertThat(config.getBaseUrl())
                .isEqualTo(githubMockServer.getBaseUrl());
        assertThat(config.getTokenTtl())
                .isEqualTo(Duration.ofHours(1));
        assertThat(config.getTokenMaxTtl())
                .isEqualTo(Duration.ofHours(2));
    }

    @Test
    public void testTeamMappings(VaultClient client, @Random String team) throws Exception {
        var githubApi = client.auth().github();

        var team1 = team.toLowerCase() + "1";
        var team2 = team.toLowerCase() + "2";

        githubApi.updateTeamMapping(team1, "default,dev")
                .toCompletableFuture().get();
        githubApi.updateTeamMapping(team2, "prod")
                .toCompletableFuture().get();

        var mapping = githubApi.readTeamMapping(team1)
                .toCompletableFuture().get();

        assertThat(mapping.getKey())
                .isEqualTo(team1);
        assertThat(mapping.getValue())
                .isEqualTo("default,dev");

        var teams = githubApi.listTeamMappings()
                .toCompletableFuture().get();

        assertThat(teams)
                .contains(team1, team2);

        githubApi.deleteTeamMapping(team1)
                .toCompletableFuture().get();

        var teams2 = githubApi.listTeamMappings()
                .toCompletableFuture().get();

        assertThat(teams2)
                .doesNotContain(team1)
                .contains(team2);
    }

    @Test
    public void testUserMappings(VaultClient client, @Random String user) throws Exception {
        var githubApi = client.auth().github();

        var user1 = user.toLowerCase() + "1";
        var user2 = user.toLowerCase() + "2";

        githubApi.updateUserMapping(user1, "default,dev")
                .toCompletableFuture().get();
        githubApi.updateUserMapping(user2, "prod")
                .toCompletableFuture().get();

        var mapping = githubApi.readUserMapping(user1)
                .toCompletableFuture().get();

        assertThat(mapping.getKey())
                .isEqualTo(user1);
        assertThat(mapping.getValue())
                .isEqualTo("default,dev");

        var users = githubApi.listUserMappings()
                .toCompletableFuture().get();

        assertThat(users)
                .contains(user1, user2);

        githubApi.deleteUserMapping(user1)
                .toCompletableFuture().get();

        var users2 = githubApi.listUserMappings()
                .toCompletableFuture().get();

        assertThat(users2)
                .doesNotContain(user1)
                .contains(user2);
    }

    @Test
    public void testLoginProcess(VaultClient client) throws Exception {
        var githubApi = client.auth().github();

        githubApi.configure(new VaultAuthGithubConfigureParams()
                .setOrganization(GithubMockServer.ORGANIZATION)
                .setOrganizationId(GithubMockServer.ORGANIZATION_ID)
                .setBaseUrl(githubMockServer.getBaseUrl()))
                .toCompletableFuture().get();

        githubApi.updateTeamMapping(GithubMockServer.TEAM_SLUG, "dev-policy")
                .toCompletableFuture().get();
        githubApi.updateUserMapping(GithubMockServer.USERNAME, "bob-policy")
                .toCompletableFuture().get();

        var login = githubApi.login(GithubMockServer.TOKEN)
                .toCompletableFuture().get();

        assertThat(login.getClientToken())
                .isNotNull();
        assertThat(login.getMetadata().getUsername())
                .isEqualTo(GithubMockServer.USERNAME);
        assertThat(login.getMetadata().getOrg())
                .isEqualTo(GithubMockServer.ORGANIZATION);
        assertThat(login.getPolicies())
                .contains("dev-policy", "bob-policy");
    }

    @Test
    public void testLoginProcessWithInvalidToken(VaultClient client) throws Exception {
        var githubApi = client.auth().github();

        githubApi.configure(new VaultAuthGithubConfigureParams()
                .setOrganization(GithubMockServer.ORGANIZATION)
                .setOrganizationId(GithubMockServer.ORGANIZATION_ID)
                .setBaseUrl(githubMockServer.getBaseUrl()))
                .toCompletableFuture().get();

        assertThatThrownBy(() -> githubApi.login("wrong-github-token").toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasMessageContaining("Bad credentials");
    }

    @Test
    public void testClientLogin(VaultClient client) throws Exception {
        var githubApi = client.auth().github();

        var userPolicy = GithubMockServer.USERNAME + "-policy";

        // Add policy to read sys/mounts/* path
        client.sys().policy().update(userPolicy, """
                path "sys/mounts/*" {
                    capabilities = [ "read" ]
                }""")
                .toCompletableFuture().get();

        githubApi.configure(new VaultAuthGithubConfigureParams()
                .setOrganization(GithubMockServer.ORGANIZATION)
                .setOrganizationId(GithubMockServer.ORGANIZATION_ID)
                .setBaseUrl(githubMockServer.getBaseUrl()))
                .toCompletableFuture().get();

        githubApi.updateUserMapping(GithubMockServer.USERNAME, userPolicy)
                .toCompletableFuture().get();

        // Login
        var authClient = client.configure()
                .github(GithubMockServer.TOKEN)
                .build();

        // Validate

        assertThatThrownBy(() -> authClient.sys().auth().read("token").toCompletableFuture().get())
                .isInstanceOf(ExecutionException.class).cause()
                .isInstanceOf(VaultClientException.class)
                .hasMessageContaining("permission denied");

        var mountInfo = authClient.sys().mounts().read("secret")
                .toCompletableFuture().get();

        assertThat(mountInfo.getDescription())
                .isNotNull();
    }

}

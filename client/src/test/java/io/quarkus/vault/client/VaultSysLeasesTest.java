package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.userpass.VaultAuthUserPassUpdateUserParams;
import io.quarkus.vault.client.api.common.VaultTokenType;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest(auths = {
        @VaultClientTest.Mount(path = "userpass", type = "userpass"),
})
public class VaultSysLeasesTest {

    @Test
    public void testList(VaultClient client) {
        var leasesApi = client.sys().leases();

        var leases = leasesApi.list("auth")
                .await().indefinitely();

        assertThat(leases)
                .contains("token/");
    }

    @Test
    public void testRead(VaultClient client) {
        var leasesApi = client.sys().leases();

        client.auth().userPass().updateUser("test", new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenType(VaultTokenType.SERVICE)
                .setTokenTtl(Duration.ofMinutes(5)))
                .await().indefinitely();

        client.auth().userPass().login("test", "test")
                .await().indefinitely();

        var leases = client.sys().leases().list("auth/userpass/login/test/")
                .await().indefinitely();

        var leaseId = "auth/userpass/login/test/" + leases.get(0);
        var lease = leasesApi.read(leaseId)
                .await().indefinitely();

        assertThat(lease)
                .isNotNull();
        assertThat(lease.getId())
                .isEqualTo(leaseId);
        assertThat(lease.getIssueTime())
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(lease.getLastRenewalTime())
                .isNull();
        assertThat(lease.getExpireTime())
                .isBetween(now().plusMinutes(5).minusSeconds(2), now().plusMinutes(5).plusSeconds(2));
        assertThat(lease.isRenewable())
                .isTrue();
    }

    @Test
    public void testRevoke(VaultClient client) {
        var leasesApi = client.sys().leases();

        client.auth().userPass().updateUser("test", new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenType(VaultTokenType.SERVICE)
                .setTokenTtl(Duration.ofMinutes(5)))
                .await().indefinitely();

        client.auth().userPass().login("test", "test")
                .await().indefinitely();

        var leases = client.sys().leases().list("auth/userpass/login/test/")
                .await().indefinitely();

        var leaseId = "auth/userpass/login/test/" + leases.get(0);

        assertThatCode(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .doesNotThrowAnyException();

        leasesApi.revoke(leaseId, true)
                .await().indefinitely();

        assertThatThrownBy(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .asString().contains("status=400");
    }

    @Test
    public void testRevokeForce(VaultClient client) {
        var leasesApi = client.sys().leases();

        client.auth().userPass().updateUser("test", new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenType(VaultTokenType.SERVICE)
                .setTokenTtl(Duration.ofMinutes(5)))
                .await().indefinitely();

        client.auth().userPass().login("test", "test")
                .await().indefinitely();

        var leases = client.sys().leases().list("auth/userpass/login/test/")
                .await().indefinitely();

        var leaseId = "auth/userpass/login/test/" + leases.get(0);

        assertThatCode(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .doesNotThrowAnyException();

        leasesApi.revokeForce("auth/userpass/login/test/")
                .await().indefinitely();

        assertThatThrownBy(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .asString().contains("status=400");
    }

    @Test
    public void testRevokePrefix(VaultClient client) {
        var leasesApi = client.sys().leases();

        client.auth().userPass().updateUser("test", new VaultAuthUserPassUpdateUserParams()
                .setPassword("test")
                .setTokenType(VaultTokenType.SERVICE)
                .setTokenTtl(Duration.ofMinutes(5)))
                .await().indefinitely();

        client.auth().userPass().login("test", "test")
                .await().indefinitely();

        var leases = client.sys().leases().list("auth/userpass/login/test/")
                .await().indefinitely();

        var leaseId = "auth/userpass/login/test/" + leases.get(0);

        assertThatCode(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .doesNotThrowAnyException();

        leasesApi.revokePrefix("auth/userpass/login/test/")
                .await().indefinitely();

        assertThatThrownBy(() -> leasesApi.read(leaseId)
                .await().indefinitely())
                .asString().contains("status=400");
    }

    @Test
    public void testTidy(VaultClient client) {
        var leasesApi = client.sys().leases();

        assertThatCode(() -> leasesApi.tidy()
                .await().indefinitely())
                .doesNotThrowAnyException();
    }
}

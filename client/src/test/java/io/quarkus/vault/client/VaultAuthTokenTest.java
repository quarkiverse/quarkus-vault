package io.quarkus.vault.client;

import static java.time.OffsetDateTime.now;
import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.quarkus.vault.client.api.auth.token.VaultAuthTokenCreateTokenParams;
import io.quarkus.vault.client.api.auth.token.VaultAuthTokenUpdateRoleParams;
import io.quarkus.vault.client.test.Random;
import io.quarkus.vault.client.test.VaultClientTest;

@VaultClientTest
public class VaultAuthTokenTest {

    @Test
    public void testListAccessors(VaultClient client) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(false, null)
                .await().indefinitely();

        var accessors = tokenApi.listAccessors()
                .await().indefinitely();

        assertThat(accessors)
                .contains(createdToken.accessor);
    }

    @Test
    public void testCreateToken(VaultClient client) {
        var tokenApi = client.auth().token();

        var tokenInfo = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setMeta(Map.of("foo", "bar")))
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.clientToken)
                .isNotNull();
        assertThat(tokenInfo.accessor)
                .isNotNull();
        assertThat(tokenInfo.policies)
                .contains("root");
        assertThat(tokenInfo.tokenPolicies)
                .contains("root");
        assertThat(tokenInfo.metadata)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.leaseDuration)
                .isNotNull();
        assertThat(tokenInfo.renewable)
                .isFalse();
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.tokenType)
                .isEqualTo("service");
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.mfaRequirement)
                .isNull();
        assertThat(tokenInfo.numUses)
                .isEqualTo(0);
    }

    @Test
    public void testCreateTokenWithRole(VaultClient client, @Random String role) {
        var tokenApi = client.auth().token();

        tokenApi.updateRole(role, new VaultAuthTokenUpdateRoleParams()
                .setAllowedPolicies(List.of("root")))
                .await().indefinitely();

        var createdToken = tokenApi.create(role, new VaultAuthTokenCreateTokenParams()
                .setRoleName(role))
                .await().indefinitely();

        var tokenInfo = tokenApi.lookup(createdToken.clientToken)
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.role)
                .isEqualTo(role);
    }

    @Test
    public void testLookupToken(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var policy = tokenId + "-policy";

        client.sys().policy().update(policy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setPolicies(List.of(policy))
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var tokenInfo = tokenApi.lookup(createdToken.clientToken)
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.creationTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(tokenInfo.creationTtl)
                .isEqualTo(60);
        assertThat(tokenInfo.displayName)
                .isEqualTo("token-My-Token");
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.expireTime)
                .isBetween(now().plusSeconds(58), now().plusSeconds(62));
        assertThat(tokenInfo.explicitMaxTtl)
                .isEqualTo(0);
        assertThat(tokenInfo.id)
                .isEqualTo(tokenId);
        assertThat(tokenInfo.identityPolicies)
                .isNull();
        assertThat(tokenInfo.issueTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(tokenInfo.meta)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.numUses)
                .isEqualTo(5);
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.path)
                .isEqualTo("auth/token/create");
        assertThat(tokenInfo.policies)
                .contains(policy);
        assertThat(tokenInfo.renewable)
                .isTrue();
        assertThat(tokenInfo.ttl)
                .isBetween(57L, 60L);
        assertThat(tokenInfo.role)
                .isNull();
        assertThat(tokenInfo.type)
                .isEqualTo("service");
    }

    @Test
    public void testLookupSelfToken(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var policy = tokenId + "-policy";

        client.sys().policy().update(policy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setPolicies(List.of(policy))
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var tokenClient = client.configure().clientToken(createdToken.clientToken).build();

        var tokenInfo = tokenClient.auth().token().lookupSelf()
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.creationTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(tokenInfo.creationTtl)
                .isEqualTo(60);
        assertThat(tokenInfo.displayName)
                .isEqualTo("token-My-Token");
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.expireTime)
                .isBetween(now().plusSeconds(58), now().plusSeconds(62));
        assertThat(tokenInfo.explicitMaxTtl)
                .isEqualTo(0);
        assertThat(tokenInfo.id)
                .isEqualTo(tokenId);
        assertThat(tokenInfo.identityPolicies)
                .isNull();
        assertThat(tokenInfo.issueTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(tokenInfo.meta)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.numUses)
                .isEqualTo(4);
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.path)
                .isEqualTo("auth/token/create");
        assertThat(tokenInfo.policies)
                .contains(policy);
        assertThat(tokenInfo.renewable)
                .isTrue();
        assertThat(tokenInfo.ttl)
                .isBetween(57L, 60L);
    }

    @Test
    public void testLookupTokenAccessor(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var accessorInfo = tokenApi.lookupAccessor(createdToken.accessor)
                .await().indefinitely();

        assertThat(accessorInfo)
                .isNotNull();
        assertThat(accessorInfo.accessor)
                .isEqualTo(createdToken.accessor);
        assertThat(accessorInfo.creationTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(accessorInfo.creationTtl)
                .isEqualTo(60);
        assertThat(accessorInfo.displayName)
                .isEqualTo("token-My-Token");
        assertThat(accessorInfo.entityId)
                .isEmpty();
        assertThat(accessorInfo.expireTime)
                .isBetween(now().plusSeconds(58), now().plusSeconds(62));
        assertThat(accessorInfo.explicitMaxTtl)
                .isEqualTo(0);
        assertThat(accessorInfo.id)
                .isEmpty();
        assertThat(accessorInfo.identityPolicies)
                .isNull();
        assertThat(accessorInfo.issueTime)
                .isBetween(now().minusSeconds(2), now().plusSeconds(2));
        assertThat(accessorInfo.meta)
                .containsEntry("foo", "bar");
        assertThat(accessorInfo.numUses)
                .isEqualTo(5);
        assertThat(accessorInfo.orphan)
                .isFalse();
        assertThat(accessorInfo.path)
                .isEqualTo("auth/token/create");
        assertThat(accessorInfo.policies)
                .contains("root");
        assertThat(accessorInfo.renewable)
                .isTrue();
        assertThat(accessorInfo.ttl)
                .isBetween(57L, 60L);
    }

    @Test
    public void testRenewToken(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var policy = tokenId + "-policy";

        client.sys().policy().update(policy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setPolicies(List.of(policy))
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var tokenInfo = tokenApi.renew(createdToken.clientToken, "30s")
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.clientToken)
                .isNotNull();
        assertThat(tokenInfo.accessor)
                .isNotNull();
        assertThat(tokenInfo.policies)
                .contains(policy);
        assertThat(tokenInfo.tokenPolicies)
                .contains(policy);
        assertThat(tokenInfo.metadata)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.leaseDuration)
                .isEqualTo(30);
        assertThat(tokenInfo.renewable)
                .isTrue();
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.tokenType)
                .isEqualTo("service");
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.mfaRequirement)
                .isNull();
        assertThat(tokenInfo.numUses)
                .isEqualTo(5);
    }

    @Test
    public void testRenewSelfToken(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var policy = tokenId + "-policy";

        client.sys().policy().update(policy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setPolicies(List.of(policy))
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var tokenClient = client.configure().clientToken(createdToken.clientToken).build();

        var tokenInfo = tokenClient.auth().token().renewSelf("30s")
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.clientToken)
                .isNotNull();
        assertThat(tokenInfo.accessor)
                .isNotNull();
        assertThat(tokenInfo.policies)
                .contains(policy);
        assertThat(tokenInfo.tokenPolicies)
                .contains(policy);
        assertThat(tokenInfo.metadata)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.leaseDuration)
                .isEqualTo(30);
        assertThat(tokenInfo.renewable)
                .isTrue();
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.tokenType)
                .isEqualTo("service");
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.mfaRequirement)
                .isNull();
        assertThat(tokenInfo.numUses)
                .isEqualTo(5);
    }

    @Test
    public void testRenewAccessor(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var policy = tokenId + "-policy";

        client.sys().policy().update(policy, """
                path "secret/*" {
                    capabilities = [ "read" ]
                }""")
                .await().indefinitely();

        var createdToken = tokenApi.create(false, new VaultAuthTokenCreateTokenParams()
                .setId(tokenId)
                .setDisplayName("My Token")
                .setPolicies(List.of(policy))
                .setMeta(Map.of("foo", "bar"))
                .setTtl("1m")
                .setNumUses(5))
                .await().indefinitely();

        var tokenInfo = tokenApi.renewAccessor(createdToken.accessor, "30s")
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();
        assertThat(tokenInfo.clientToken)
                .isNotNull();
        assertThat(tokenInfo.accessor)
                .isNotNull();
        assertThat(tokenInfo.policies)
                .contains(policy);
        assertThat(tokenInfo.tokenPolicies)
                .contains(policy);
        assertThat(tokenInfo.metadata)
                .containsEntry("foo", "bar");
        assertThat(tokenInfo.leaseDuration)
                .isEqualTo(30);
        assertThat(tokenInfo.renewable)
                .isTrue();
        assertThat(tokenInfo.entityId)
                .isEmpty();
        assertThat(tokenInfo.tokenType)
                .isEqualTo("service");
        assertThat(tokenInfo.orphan)
                .isFalse();
        assertThat(tokenInfo.mfaRequirement)
                .isNull();
        assertThat(tokenInfo.numUses)
                .isEqualTo(5);
    }

    @Test
    public void testRevokeToken(VaultClient client) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(false, null)
                .await().indefinitely();

        tokenApi.revoke(createdToken.clientToken)
                .await().indefinitely();

        assertThatThrownBy(() -> tokenApi.lookup(createdToken.clientToken)
                .await().indefinitely())
                .isInstanceOf(VaultException.class)
                .asString().contains("status=403");
    }

    @Test
    public void testRevokeSelfToken(VaultClient client) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(false, null)
                .await().indefinitely();

        var tokenClient = client.configure().clientToken(createdToken.clientToken).build();

        tokenClient.auth().token().revokeSelf()
                .await().indefinitely();

        assertThatThrownBy(() -> client.auth().token().lookup(createdToken.clientToken)
                .await().indefinitely())
                .isInstanceOf(VaultException.class)
                .asString().contains("status=403");
    }

    @Test
    public void testRevokeOrphanToken(VaultClient client) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(null)
                .await().indefinitely();

        tokenApi.revoke(true, createdToken.clientToken)
                .await().indefinitely();

        assertThatThrownBy(() -> tokenApi.lookup(createdToken.clientToken)
                .await().indefinitely())
                .isInstanceOf(VaultException.class)
                .asString().contains("status=403");
    }

    @Test
    public void testRevokeAccessor(VaultClient client) {
        var tokenApi = client.auth().token();

        var createdToken = tokenApi.create(null)
                .await().indefinitely();

        tokenApi.lookupAccessor(createdToken.accessor)
                .await().indefinitely();

        tokenApi.revokeAccessor(createdToken.accessor)
                .await().indefinitely();

        var accessors = tokenApi.listAccessors()
                .await().indefinitely();

        assertThat(accessors)
                .doesNotContain(createdToken.accessor);
    }

    @Test
    public void testUpdateTokenRole(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var role = tokenId + "-role";
        var policy = tokenId + "-policy";

        client.auth().token().updateRole(role, new VaultAuthTokenUpdateRoleParams()
                .setAllowedPolicies(List.of(policy))
                .setDisallowedPolicies(List.of("default"))
                .setAllowedPoliciesGlob(List.of(policy + "*"))
                .setDisallowedPoliciesGlob(List.of("default*"))
                .setOrphan(true)
                .setRenewable(false)
                .setPathSuffix("foo")
                .setAllowedEntityAliases(List.of("foo", "bar"))
                .setTokenBoundCidrs(List.of("127.0.0.1/32"))
                .setTokenExplicitMaxTtl(120L)
                .setTokenNoDefaultPolicy(true)
                .setTokenNumUses(5)
                .setTokenPeriod("90s")
                .setTokenType("service"))
                .await().indefinitely();

        var roleInfo = tokenApi.readRole(role)
                .await().indefinitely();

        assertThat(roleInfo)
                .isNotNull();
        assertThat(roleInfo.allowedEntityAliases)
                .contains("foo", "bar");
        assertThat(roleInfo.allowedPolicies)
                .contains(policy);
        assertThat(roleInfo.disallowedPolicies)
                .contains("default");
        assertThat(roleInfo.allowedPoliciesGlob)
                .contains(policy + "*");
        assertThat(roleInfo.disallowedPoliciesGlob)
                .contains("default*");
        assertThat(roleInfo.explicitMaxTtl)
                .isEqualTo(0);
        assertThat(roleInfo.name)
                .isEqualTo(role);
        assertThat(roleInfo.orphan)
                .isTrue();
        assertThat(roleInfo.pathSuffix)
                .isEqualTo("foo");
        assertThat(roleInfo.period)
                .isEqualTo(0);
        assertThat(roleInfo.renewable)
                .isFalse();
        assertThat(roleInfo.tokenExplicitMaxTtl)
                .isEqualTo(120);
        assertThat(roleInfo.tokenNoDefaultPolicy)
                .isTrue();
        assertThat(roleInfo.tokenPeriod)
                .isEqualTo(90);
        assertThat(roleInfo.tokenType)
                .isEqualTo("service");
    }

    @Test
    public void testDeleteTokenRole(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var role = tokenId + "-role";

        tokenApi.updateRole(role, null)
                .await().indefinitely();

        var tokenInfo = tokenApi.readRole(role)
                .await().indefinitely();

        assertThat(tokenInfo)
                .isNotNull();

        tokenApi.deleteRole(role)
                .await().indefinitely();

        assertThatThrownBy(() -> tokenApi.readRole(role)
                .await().indefinitely())
                .isInstanceOf(VaultException.class)
                .asString().contains("status=404");
    }

    @Test
    public void testListTokenRoles(VaultClient client, @Random String tokenId) {
        var tokenApi = client.auth().token();

        var role = tokenId + "-role";

        tokenApi.updateRole(role, null)
                .await().indefinitely();

        var roles = tokenApi.listRoles()
                .await().indefinitely();

        assertThat(roles)
                .contains(role);
    }

    @Test
    public void testTidyTokens(VaultClient client) {
        var tokenApi = client.auth().token();

        assertThatCode(() -> tokenApi.tidyTokens().await().indefinitely())
                .doesNotThrowAnyException();
    }

}

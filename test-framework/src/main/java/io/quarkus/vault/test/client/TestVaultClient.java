package io.quarkus.vault.test.client;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.TlsConfig;
import io.quarkus.vault.runtime.VaultConfigHolder;
import io.quarkus.vault.runtime.client.PrivateVertxVaultClient;
import io.quarkus.vault.runtime.client.dto.transit.VaultTransitRandomBody;
import io.quarkus.vault.test.client.dto.VaultAppRoleRoleId;
import io.quarkus.vault.test.client.dto.VaultAppRoleSecretId;
import io.quarkus.vault.test.client.dto.VaultTransitHash;
import io.quarkus.vault.test.client.dto.VaultTransitHashBody;
import io.quarkus.vault.test.client.dto.VaultTransitRandom;
import io.smallrye.mutiny.Uni;

public class TestVaultClient extends PrivateVertxVaultClient {

    private static VaultConfigHolder getConfigHolder() {
        return Arc.container().instance(VaultConfigHolder.class).get();
    }

    public TestVaultClient() {
        this(getConfigHolder());
    }

    public TestVaultClient(VaultConfigHolder configHolder) {
        super(configHolder, new TlsConfig());
    }

    public Uni<VaultAppRoleSecretId> generateAppRoleSecretId(String token, String roleName) {
        return post("", "auth/approle/role/" + roleName + "/secret-id", token, null, VaultAppRoleSecretId.class);
    }

    public Uni<VaultAppRoleRoleId> getAppRoleRoleId(String token, String role) {
        return get("", "auth/approle/role/" + role + "/role-id", token, VaultAppRoleRoleId.class);
    }

    public Uni<Void> rotate(String token, String keyName) {
        return post("", "transit/keys/" + keyName + "/rotate", token, null, null, 204);
    }

    public Uni<VaultTransitRandom> generateRandom(String token, int byteCount, VaultTransitRandomBody body) {
        return post("", "transit/random/" + byteCount, token, body, VaultTransitRandom.class);
    }

    public Uni<VaultTransitHash> hash(String token, String hashAlgorithm, VaultTransitHashBody body) {
        return post("", "transit/hash/" + hashAlgorithm, token, body, VaultTransitHash.class);
    }
}

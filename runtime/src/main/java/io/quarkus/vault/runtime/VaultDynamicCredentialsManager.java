package io.quarkus.vault.runtime;

import static io.quarkus.credentials.CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import io.quarkus.vault.client.common.VaultModel;
import jakarta.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.client.VaultClient;
import io.quarkus.vault.client.VaultClientException;
import io.quarkus.vault.client.api.common.VaultAuthResult;
import io.quarkus.vault.client.api.common.VaultLeasedResult;
import io.quarkus.vault.client.common.VaultLeasedResultExtractor;
import io.quarkus.vault.client.common.VaultRequest;
import io.quarkus.vault.client.common.VaultResponse;
import io.quarkus.vault.runtime.config.VaultRuntimeConfig;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultDynamicCredentialsManager {

    private static final Logger log = Logger.getLogger(VaultDynamicCredentialsManager.class.getName());

    private final ConcurrentHashMap<String, VaultDynamicCredentials> credentialsCache = new ConcurrentHashMap<>();
    private final VaultClient vaultClient;
    private final VaultConfigHolder vaultConfigHolder;

    public VaultDynamicCredentialsManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder) {
        this.vaultClient = vaultClient;
        this.vaultConfigHolder = vaultConfigHolder;
    }

    private String getCredentialsPath(String mount, String requestPath) {
        return mount + "/" + requestPath;
    }

    private String getCredentialsCacheKey(String mount, String requestPath, String role) {
        return getCredentialsPath(mount, requestPath) + "@" + role;
    }

    VaultDynamicCredentials getCachedCredentials(String mount, String requestPath, String role) {
        return credentialsCache.get(getCredentialsCacheKey(mount, requestPath, role));
    }

    void putCachedCredentials(String mount, String requestPath, String role, VaultDynamicCredentials credentials) {
        credentialsCache.put(getCredentialsCacheKey(mount, requestPath, role), credentials);
    }

    private VaultRuntimeConfig getConfig() {
        return vaultConfigHolder.getVaultRuntimeConfig();
    }

    public Uni<Map<String, String>> getDynamicCredentials(String mount, String requestPath, String role) {
        VaultDynamicCredentials currentCredentials = getCachedCredentials(mount, requestPath, role);
        return getCredentials(currentCredentials, mount, requestPath, role)
                .map(credentials -> {
                    putCachedCredentials(mount, requestPath, role, credentials);
                    Map<String, String> properties = new HashMap<>();
                    properties.put(USER_PROPERTY_NAME, credentials.username);
                    properties.put(PASSWORD_PROPERTY_NAME, credentials.password);
                    properties.put(EXPIRATION_TIMESTAMP_PROPERTY_NAME, credentials.getExpireInstant().toString());
                    return properties;
                });
    }

    public Uni<VaultDynamicCredentials> getCredentials(VaultDynamicCredentials currentCredentials, String mount,
            String requestPath, String role) {
        return Uni.createFrom().item(Optional.ofNullable(currentCredentials))
                // check lease is still valid
                .flatMap(this::validate)
                // extend lease if necessary
                .flatMap(credentials -> {
                    if (credentials.isPresent() && credentials.get().shouldExtend(getConfig().renewGracePeriod())) {
                        return extend(credentials.get(), mount, requestPath, role).map(Optional::of);
                    }
                    return Uni.createFrom().item(credentials);
                })
                // create lease if necessary
                .flatMap(credentials -> {
                    if (credentials.isEmpty() || credentials.get().isExpired()
                            || credentials.get().expiresSoon(getConfig().renewGracePeriod())) {
                        return create(mount, requestPath, role);
                    }
                    return Uni.createFrom().item(credentials.get());
                });
    }

    private Uni<Optional<VaultDynamicCredentials>> validate(Optional<VaultDynamicCredentials> credentials) {
        if (credentials.isEmpty()) {
            return Uni.createFrom().item(Optional.empty());
        }
        return vaultClient.sys().leases().read(credentials.get().leaseId)
                .map(ignored -> credentials)
                .onFailure(VaultClientException.class).recoverWithUni(e -> {
                    if (((VaultClientException) e).getStatus() == 400) { // bad request
                        log.debug("lease " + credentials.get().leaseId + " has become invalid");
                        return Uni.createFrom().item(Optional.empty());
                    } else {
                        return Uni.createFrom().failure(e);
                    }
                });
    }

    private Uni<VaultDynamicCredentials> extend(VaultDynamicCredentials currentCredentials, String mount, String requestPath,
            String role) {
        return vaultClient.sys().leases().renew(currentCredentials.leaseId, null)
                .map(vaultRenewLease -> {
                    LeaseBase lease = new LeaseBase(vaultRenewLease.getLeaseId(),
                            vaultRenewLease.isRenewable(),
                            vaultRenewLease.getLeaseDuration().toSeconds());
                    VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease, currentCredentials.username,
                            currentCredentials.password);
                    sanityCheck(credentials, mount, requestPath, role);
                    log.debug("extended " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                            + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel()));
                    return credentials;
                });
    }

    static class VaultDynamicCredentialsData implements VaultModel {
        public String username;
        public String password;
    }

    static class VaultDynamicCredentialsResult extends VaultLeasedResult<VaultDynamicCredentialsData, VaultAuthResult<Object>> {
    }

    private Uni<VaultDynamicCredentials> create(String mount, String requestPath, String role) {
        var request = VaultRequest.get(String.format("[DYN-CREDS (%s)] Generate for %s", mount, role))
                .path(mount, requestPath, role)
                .expectOkStatus()
                .build(VaultLeasedResultExtractor.of(VaultDynamicCredentialsResult.class));
        return vaultClient.execute(request)
                .map(VaultResponse::getResult)
                .map(vaultDynamicCredentials -> {
                    var data = vaultDynamicCredentials.getData();
                    LeaseBase lease = new LeaseBase(vaultDynamicCredentials.getLeaseId(), vaultDynamicCredentials.isRenewable(),
                            vaultDynamicCredentials.getLeaseDuration().toSeconds());
                    VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease, data.username, data.password);
                    log.debug("generated " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                            + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel()));
                    sanityCheck(credentials, mount, requestPath, role);
                    return credentials;
                });
    }

    private void sanityCheck(VaultDynamicCredentials credentials, String mount, String requestPath, String role) {
        credentials.leaseDurationSanityCheck(role + " (" + getCredentialsPath(mount, requestPath) + ")",
                getConfig().renewGracePeriod());
    }
}

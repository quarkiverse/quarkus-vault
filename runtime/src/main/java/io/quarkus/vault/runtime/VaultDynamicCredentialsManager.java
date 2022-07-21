package io.quarkus.vault.runtime;

import static io.quarkus.credentials.CredentialsProvider.EXPIRATION_TIMESTAMP_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.PASSWORD_PROPERTY_NAME;
import static io.quarkus.credentials.CredentialsProvider.USER_PROPERTY_NAME;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Singleton;

import org.jboss.logging.Logger;

import io.quarkus.vault.runtime.client.VaultClient;
import io.quarkus.vault.runtime.client.VaultClientException;
import io.quarkus.vault.runtime.client.backend.VaultInternalSystemBackend;
import io.quarkus.vault.runtime.client.secretengine.VaultInternalDynamicCredentialsSecretEngine;
import io.quarkus.vault.runtime.config.VaultBootstrapConfig;
import io.smallrye.mutiny.Uni;

@Singleton
public class VaultDynamicCredentialsManager {

    private static final Logger log = Logger.getLogger(VaultDynamicCredentialsManager.class.getName());

    private ConcurrentHashMap<String, VaultDynamicCredentials> credentialsCache = new ConcurrentHashMap<>();
    private VaultClient vaultClient;
    private VaultAuthManager vaultAuthManager;
    private VaultConfigHolder vaultConfigHolder;
    private VaultInternalSystemBackend vaultInternalSystemBackend;
    private VaultInternalDynamicCredentialsSecretEngine vaultInternalDynamicCredentialsSecretEngine;

    public VaultDynamicCredentialsManager(VaultClient vaultClient, VaultConfigHolder vaultConfigHolder,
            VaultAuthManager vaultAuthManager,
            VaultInternalSystemBackend vaultInternalSystemBackend,
            VaultInternalDynamicCredentialsSecretEngine vaultInternalDynamicCredentialsSecretEngine) {
        this.vaultClient = vaultClient;
        this.vaultConfigHolder = vaultConfigHolder;
        this.vaultAuthManager = vaultAuthManager;
        this.vaultInternalSystemBackend = vaultInternalSystemBackend;
        this.vaultInternalDynamicCredentialsSecretEngine = vaultInternalDynamicCredentialsSecretEngine;
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

    private VaultBootstrapConfig getConfig() {
        return vaultConfigHolder.getVaultBootstrapConfig();
    }

    public Uni<Map<String, String>> getDynamicCredentials(String mount, String requestPath, String role) {
        return vaultAuthManager.getClientToken(vaultClient).flatMap(token -> {
            VaultDynamicCredentials currentCredentials = getCachedCredentials(mount, requestPath, role);
            return getCredentials(currentCredentials, token, mount, requestPath, role)
                    .map(credentials -> {
                        putCachedCredentials(mount, requestPath, role, credentials);
                        Map<String, String> properties = new HashMap<>();
                        properties.put(USER_PROPERTY_NAME, credentials.username);
                        properties.put(PASSWORD_PROPERTY_NAME, credentials.password);
                        properties.put(EXPIRATION_TIMESTAMP_PROPERTY_NAME, credentials.getExpireInstant().toString());
                        return properties;
                    });
        });
    }

    public Uni<VaultDynamicCredentials> getCredentials(VaultDynamicCredentials currentCredentials,
            String clientToken, String mount, String requestPath, String role) {
        return Uni.createFrom().item(Optional.ofNullable(currentCredentials))
                // check lease is still valid
                .flatMap(credentials -> validate(credentials, clientToken))
                // extend lease if necessary
                .flatMap(credentials -> {
                    if (credentials.isPresent() && credentials.get().shouldExtend(getConfig().renewGracePeriod)) {
                        return extend(credentials.get(), clientToken, mount, requestPath, role).map(Optional::of);
                    }
                    return Uni.createFrom().item(credentials);
                })
                // create lease if necessary
                .flatMap(credentials -> {
                    if (credentials.isEmpty() || credentials.get().isExpired()
                            || credentials.get().expiresSoon(getConfig().renewGracePeriod)) {
                        return create(clientToken, mount, requestPath, role);
                    }
                    return Uni.createFrom().item(credentials.get());
                });
    }

    private Uni<Optional<VaultDynamicCredentials>> validate(Optional<VaultDynamicCredentials> credentials, String clientToken) {
        if (credentials.isEmpty()) {
            return Uni.createFrom().item(Optional.empty());
        }
        return vaultInternalSystemBackend.lookupLease(vaultClient, clientToken, credentials.get().leaseId)
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

    private Uni<VaultDynamicCredentials> extend(VaultDynamicCredentials currentCredentials, String clientToken,
            String mount, String requestPath, String role) {
        return vaultInternalSystemBackend.renewLease(vaultClient, clientToken, currentCredentials.leaseId)
                .map(vaultRenewLease -> {
                    LeaseBase lease = new LeaseBase(vaultRenewLease.leaseId,
                            vaultRenewLease.renewable,
                            vaultRenewLease.leaseDurationSecs);
                    VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease, currentCredentials.username,
                            currentCredentials.password);
                    sanityCheck(credentials, mount, requestPath, role);
                    log.debug("extended " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                            + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
                    return credentials;
                });
    }

    private Uni<VaultDynamicCredentials> create(String clientToken, String mount, String requestPath, String role) {
        return vaultInternalDynamicCredentialsSecretEngine
                .generateCredentials(vaultClient, clientToken, mount, requestPath, role)
                .map(vaultDynamicCredentials -> {
                    LeaseBase lease = new LeaseBase(vaultDynamicCredentials.leaseId, vaultDynamicCredentials.renewable,
                            vaultDynamicCredentials.leaseDurationSecs);
                    VaultDynamicCredentials credentials = new VaultDynamicCredentials(lease,
                            vaultDynamicCredentials.data.username,
                            vaultDynamicCredentials.data.password);
                    log.debug("generated " + role + "(" + getCredentialsPath(mount, requestPath) + ") credentials:"
                            + credentials.getConfidentialInfo(getConfig().logConfidentialityLevel));
                    sanityCheck(credentials, mount, requestPath, role);
                    return credentials;
                });
    }

    private void sanityCheck(VaultDynamicCredentials credentials, String mount, String requestPath, String role) {
        credentials.leaseDurationSanityCheck(role + " (" + getCredentialsPath(mount, requestPath) + ")",
                getConfig().renewGracePeriod);
    }
}
